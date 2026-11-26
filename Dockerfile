# ---- Build stage: compile the app into a jar with Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Download dependencies first, so they are cached when only the source code changes
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
# Tests run separately (./mvnw test); the image build only packages the app
RUN mvn -q package -DskipTests

# ---- Run stage: only a Java runtime and the jar (smaller image) ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
# Use at most 75% of the container's memory (free hosting gives 512 MB)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
