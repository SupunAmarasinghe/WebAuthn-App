# ---- Build stage ----
FROM gradle:7.6.0-jdk11 AS build
WORKDIR /app

# Copy only necessary files first (for caching)
COPY build.gradle settings.gradle ./
COPY src ./src

# Build the JAR file
RUN gradle clean build -x test

# ---- Run stage ----
FROM openjdk:11-jdk-slim
WORKDIR /app

# Copy the JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]