# Stage 1: Build the app with Gradle
FROM eclipse-temurin:11-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Stage 2: Run the app
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]