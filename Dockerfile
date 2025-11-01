FROM gradle:9.2-jdk21 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Ensure gradlew has executable permissions
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon clean bootJar

FROM eclipse-temurin:21

RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
