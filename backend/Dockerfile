# See /SPEC.md "Local Development" and "Deployment Strategy" — this same image is used for
# `docker compose up --build` locally and for the GHCR image cd-backend.yml deploys to Render.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Dependencies first so they're cached across builds that only touch src/.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]