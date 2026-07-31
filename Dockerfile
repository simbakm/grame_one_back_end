# Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /workspace/app
COPY pom.xml ./
COPY src ./src
# Use Maven to build the jar (skip tests for faster CI builds). If you want tests run on CI remove -DskipTests
RUN mvn -B -DskipTests package

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the built jar from the builder image. The target jar may have a version in its name so use a wildcard.
COPY --from=builder /workspace/app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
