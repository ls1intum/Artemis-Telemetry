FROM node:26.13.1-alpine AS client
WORKDIR /client
COPY client/package.json client/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY client/ ./
RUN npm run build

FROM gradle:9.9.2-jdk25 AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

COPY --from=client /client/dist /home/gradle/src/client/dist

# Ensure gradlew has executable permissions
RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon clean bootJar -PskipClient

# Temurin has not published 25.0.5.1 images yet; SapMachine ships the same OpenJDK build (25.0.5.1+2).
FROM sapmachine:27.0.1.0.3-jre-ubuntu-noble

RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/* \
    && mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
