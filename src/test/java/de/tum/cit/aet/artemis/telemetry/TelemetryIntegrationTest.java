package de.tum.cit.aet.artemis.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.DriverManager;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringResourceAccessor;
import org.springframework.core.io.DefaultResourceLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;
import de.tum.cit.aet.artemis.telemetry.service.TelemetryService;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryDTO;

@SpringBootTest(properties = {"telemetry.user=test", "telemetry.password=test", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc
class TelemetryIntegrationTest {
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:26.7.0");
    static {
        MYSQL.start();
        try (var connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
            var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (var liquibase = new Liquibase("legacy-changelog.xml", new SpringResourceAccessor(new DefaultResourceLoader()), database)) {
                liquibase.update(new liquibase.Contexts());
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("""
                        INSERT INTO telemetry(server_url,version,operator_name,profiles,timestamp,is_test_server) VALUES
                        ('HTTPS://LEGACY.EXAMPLE:443/','old','University','prod','2025-01-01',false),
                        ('https://legacy.example','new','University','prod','2025-02-01',false),
                        ('https://legacy.example/Case','case','University','prod','2025-02-01',false),
                        ('https://legacy.example/case','case','University','prod','2025-02-01',false),
                        ('invalid-url','old','University',NULL,NULL,false),
                        ('https://old-test.example','test','University','prod','2025-02-01',true)
                        """);
                    connection.commit();
                }
            }
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired TelemetryService service;
    @Autowired JsonMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;

    TelemetryDTO report(String url, String id, ZonedDateTime startedAt) {
        return mapper.readValue("""
                {"version":"10.0.0","serverUrl":"%s","operator":"University","profiles":["prod","core","scheduling"],
                 "startupId":"%s","startedAt":"%s","moduleFeatures":["iris","exam"],"numberOfNodes":2,"buildAgentCount":3,"isMultiNode":true}
                """.formatted(url, id, startedAt), TelemetryDTO.class);
    }

    @Test
    void migratesHistoryWithoutLosingRowsOrPathCase() {
        assertThat(jdbc.queryForObject("select count(*) from telemetry where version in ('old','new','case','test')", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("select count(*) from telemetry_instance where server_url like 'https://legacy.example%'", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(distinct instance_id) from telemetry where id in (1,2)", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select latest_startup_id from telemetry_instance where server_url='https://legacy.example'", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select instance_id from telemetry where id=5", Long.class)).isNull();
        assertThat(service.getInstances(0,100).getContent()).noneMatch(i -> i.serverUrl().equals("https://old-test.example"));
        assertThat(TelemetryDTO.from(service.get(5L)).profiles()).isEmpty();
    }

    @Test
    void storesRepeatedStartupsAndDeduplicatesRetries() {
        String id = UUID.randomUUID().toString();
        var first = service.record(report("HTTPS://REPEAT.EXAMPLE:443/", id, ZonedDateTime.now().minusHours(2)));
        var retry = service.record(report("https://repeat.example", id, ZonedDateTime.now().minusHours(2)));
        var second = service.record(report("https://repeat.example", UUID.randomUUID().toString(), ZonedDateTime.now().minusHours(1)));
        assertThat(retry.getId()).isEqualTo(first.getId());
        assertThat(second.getInstanceId()).isEqualTo(first.getInstanceId());
        assertThat(service.getStartups(first.getInstanceId(),0,20).getTotalElements()).isEqualTo(2);
        assertThat(second.getModuleFeatures()).containsExactly("iris","exam");
        assertThat(second.isMultiNode()).isTrue();
    }

    @Test
    void delayedOlderReportDoesNotReplaceLatestSnapshot() {
        var newer = service.record(report("https://ordering.example",UUID.randomUUID().toString(),ZonedDateTime.now().minusHours(1)));
        service.record(report("https://ordering.example",UUID.randomUUID().toString(),ZonedDateTime.now().minusDays(1)));
        assertThat(jdbc.queryForObject("select latest_startup_id from telemetry_instance where id=?",Long.class,newer.getInstanceId())).isEqualTo(newer.getId());
    }

    @Test
    void concurrentFirstReportsShareAnInstanceAndPreserveAllStartups() throws Exception {
        try (var executor = Executors.newFixedThreadPool(6)) {
            var tasks = IntStream.range(0,6).mapToObj(i -> (Callable<Long>) () -> service.record(report("https://parallel.example",
                    UUID.randomUUID().toString(),ZonedDateTime.now().minusMinutes(1))).getInstanceId()).toList();
            var results = executor.invokeAll(tasks);
            Long instanceId = results.getFirst().get();
            for (var result : results) assertThat(result.get()).isEqualTo(instanceId);
            assertThat(service.getStartups(instanceId,0,20).getTotalElements()).isEqualTo(6);
        }
    }

    @Test
    void legacySenderKeepsUnknownCountsAndFeatures() {
        var dto = mapper.readValue("""
                {"version":"9.0","serverUrl":"https://unknown.example","operator":"University","profiles":["prod"]}
                """,TelemetryDTO.class);
        var saved = service.record(dto);
        assertThat(saved.getNumberOfNodes()).isNull();
        assertThat(saved.getBuildAgentCount()).isNull();
        assertThat(saved.isMultiNode()).isNull();
        assertThat(saved.getModuleFeatures()).isNull();
    }

    @Test
    void testReportsAreIgnoredBeforeValidation() throws Exception {
        Long count = jdbc.queryForObject("select count(*) from telemetry",Long.class);
        mvc.perform(post("/api/telemetry").contentType(MediaType.APPLICATION_JSON).content("{\"isTestServer\":true}")).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from telemetry",Long.class)).isEqualTo(count);
    }

    @Test
    void readsAreAuthenticatedAndPaginationIsBounded() throws Exception {
        mvc.perform(get("/api/telemetry/instances")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/telemetry/instances").with(httpBasic("test","test"))).andExpect(status().isOk());
        mvc.perform(get("/api/telemetry/instances?size=101").with(httpBasic("test","test"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/telemetry/instances/999999/startups").with(httpBasic("test","test"))).andExpect(status().isNotFound());
    }

    @Test
    void updatesLegacyInstallationWithMissingTimestamp() {
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("INSERT INTO telemetry (server_url,version,operator_name,profiles) VALUES ('https://undated.example','9','U','prod')");
            Long oldId = jdbc.queryForObject("SELECT id FROM telemetry WHERE server_url='https://undated.example'", Long.class);
            jdbc.update("INSERT INTO telemetry_instance(server_url,latest_startup_id) VALUES ('https://undated.example',?)", oldId);
        });
        var saved = service.record(report("https://undated.example", UUID.randomUUID().toString(), ZonedDateTime.now().minusMinutes(10)));
        assertThat(jdbc.queryForObject("SELECT latest_startup_id FROM telemetry_instance WHERE id=?", Long.class, saved.getInstanceId())).isEqualTo(saved.getId());
    }

    @Test
    void concurrentDuplicateReportsCreateOnlyOneStartup() throws Exception {
        String id = UUID.randomUUID().toString();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = IntStream.range(0,4).mapToObj(i -> (Callable<Long>) () -> service.record(report("https://duplicate.example",
                    id,ZonedDateTime.now().minusMinutes(1))).getId()).toList();
            var results = executor.invokeAll(tasks);
            Long startupId = results.getFirst().get();
            for (var result : results) assertThat(result.get()).isEqualTo(startupId);
        }
    }

    @Test
    void rejectsDatesOutsideDatabaseRange() throws Exception {
        for (String startedAt : new String[] { "0999-12-31T23:59:59Z", "1000-01-01T00:00:00+01:00" }) {
            mvc.perform(post("/api/telemetry").contentType(MediaType.APPLICATION_JSON).content("""
                    {"version":"10","serverUrl":"https://date.example","operator":"U","profiles":["prod"],"startedAt":"%s"}
                    """.formatted(startedAt))).andExpect(status().isBadRequest());
        }
    }

    @Test
    void validatesPayloadAndDoesNotTrustClientIdentity() throws Exception {
        mvc.perform(post("/api/telemetry").contentType(MediaType.APPLICATION_JSON).content("""
                {"id":1,"version":"10","serverUrl":"https://post.example","operator":"U","profiles":["prod"],"moduleFeatures":[]}
                """)).andExpect(status().isOk());
        assertThat(service.get(1L).getVersion()).isEqualTo("old");
        mvc.perform(post("/api/telemetry").contentType(MediaType.APPLICATION_JSON).content("""
                {"version":"10","serverUrl":"https://post.example?secret=x","operator":"U","profiles":["prod"]}
                """)).andExpect(status().isBadRequest());
    }

    @Test
    void acceptsMissingNullAndEmptyProfilesAndStoresNull() throws Exception {
        var payloads = List.of(
                "{\"version\":\"10\",\"serverUrl\":\"https://profiles-missing.example\",\"operator\":\"U\"}",
                "{\"version\":\"10\",\"serverUrl\":\"https://profiles-null.example\",\"operator\":\"U\",\"profiles\":null}",
                "{\"version\":\"10\",\"serverUrl\":\"https://profiles-empty.example\",\"operator\":\"U\",\"profiles\":[]}");
        for (String payload : payloads) {
            mvc.perform(post("/api/telemetry").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
        }
        assertThat(jdbc.queryForList("SELECT profiles FROM telemetry WHERE server_url LIKE 'https://profiles-%'", String.class))
                .containsExactlyInAnyOrder(null, null, null);
    }

    @Test
    void storesEmptyModuleFeaturesAsNull() throws Exception {
        mvc.perform(post("/api/telemetry").contentType(MediaType.APPLICATION_JSON).content("""
                {"version":"10","serverUrl":"https://empty-module-features.example","operator":"U","profiles":["prod"],"moduleFeatures":[]}
                """)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT module_features FROM telemetry WHERE server_url='https://empty-module-features.example'", String.class)).isNull();
    }

    @Test
    void browserSessionRequiresLoginAndSupportsLogout() throws Exception {
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        var anonymous = mvc.perform(get("/api/auth/session")).andExpect(status().isOk()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) anonymous.getRequest().getSession(false);
        var token = mapper.readTree(anonymous.getResponse().getContentAsString()).get("csrfToken").asText();
        mvc.perform(post("/api/auth/login").session(session).param("username", "test").param("password", "test"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN", token).param("username", "test").param("password", "wrong"))
                .andExpect(status().isUnauthorized());
        var login = mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN", token).param("username", "test").param("password", "test"))
                .andExpect(status().isNoContent()).andReturn();
        var authenticated = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/dashboard").session(authenticated)).andExpect(status().isOk());
        var current = mvc.perform(get("/api/auth/session").session(authenticated)).andExpect(status().isOk()).andReturn();
        var data = mapper.readTree(current.getResponse().getContentAsString());
        assertThat(data.get("authenticated").asBoolean()).isTrue();
        assertThat(data.get("username").asText()).isEqualTo("test");
        mvc.perform(post("/api/auth/logout").session(authenticated).header("X-CSRF-TOKEN", data.get("csrfToken").asText())).andExpect(status().isNoContent());
        assertThat(authenticated.isInvalid()).isTrue();
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardContainsOnlyLatestReportPerVisibleInstance() throws Exception {
        String url = "https://dashboard.example";
        service.record(report(url, UUID.randomUUID().toString(), ZonedDateTime.now().minusHours(2)));
        var latest = service.record(report(url, UUID.randomUUID().toString(), ZonedDateTime.now().minusHours(1)));
        var response = mvc.perform(get("/api/dashboard").with(httpBasic("test", "test"))).andExpect(status().isOk()).andReturn();
        var rows = mapper.readTree(response.getResponse().getContentAsString());
        var matching = new java.util.ArrayList<tools.jackson.databind.JsonNode>();
        rows.forEach(row -> { if (row.get("serverUrl").asText().equals(url)) matching.add(row); });
        assertThat(matching).hasSize(1);
        assertThat(matching.getFirst().get("latestStartup").get("id").asLong()).isEqualTo(latest.getId());
        assertThat(response.getResponse().getContentAsString()).doesNotContain("old-test.example");
    }

    @Test
    void preservesBasicChallengeWithoutTriggeringBrowserLoginPrompts() throws Exception {
        mvc.perform(get("/api/telemetry/instances")).andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("WWW-Authenticate", "Basic realm=\"Artemis Telemetry\""));
        mvc.perform(get("/api/telemetry/instances").header("X-Requested-With", "XMLHttpRequest")).andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().doesNotExist("WWW-Authenticate"));
        mvc.perform(get("/api/telemetry/instances").with(httpBasic("test", "test"))).andExpect(status().isOk());
    }

    @Test
    void dashboardDoesNotResurrectOptedOutContactDetails() throws Exception {
        var old = (tools.jackson.databind.node.ObjectNode) mapper.valueToTree(report("https://privacy-dashboard.example", UUID.randomUUID().toString(), ZonedDateTime.now().minusHours(2)));
        old.put("adminName", "Former Private Administrator");
        old.put("contact", "former-private@example.org");
        service.record(mapper.treeToValue(old, TelemetryDTO.class));
        var latest = service.record(report("https://privacy-dashboard.example", UUID.randomUUID().toString(), ZonedDateTime.now().minusHours(1)));
        var instance = service.getDashboard().stream().filter(i -> i.id().equals(latest.getInstanceId())).findFirst().orElseThrow();
        assertThat(instance.latestStartup().adminName()).isNull();
        assertThat(instance.latestStartup().contact()).isNull();
    }

}
