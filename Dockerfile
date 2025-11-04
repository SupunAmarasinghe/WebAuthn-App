# Use Eclipse Temurin JDK 11 (LTS)
FROM eclipse-temurin:11-jdk

# Set working directory
WORKDIR /app

# Copy Gradle build files
COPY build/libs/*.jar app.jar

# Expose app port
EXPOSE 8080

# Run the Spring Boot JAR
CMD ["java", "-jar", "app.jar"]
