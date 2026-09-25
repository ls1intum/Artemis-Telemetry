FROM node:24.21.0-alpine AS client
WORKDIR /client
COPY client/package.json client/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY client/ ./
RUN npm run build

FROM gradle:9.8.0-jdk25 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

COPY --from=client /client/dist /home/gradle/src/client/dist

# Ensure gradlew has executable permissions
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon clean bootJar -PskipClient

FROM eclipse-temurin:26.0.2_10-jre

RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/* \
    && mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
