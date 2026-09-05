# Multi-stage build so the final image only ships a JRE + the built jar, not
# the full Maven/JDK toolchain.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies in their own layer - only re-downloads when pom.xml changes.
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Cloud Run sets $PORT at runtime (see application.yml's server.port);
# 8080 here is just the documented default for local `docker run`.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
