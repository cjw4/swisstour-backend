# Stage 1: Build stage
# Use an official Maven image with Java 21
FROM maven:3.9.5-eclipse-temurin-21-alpine AS build

# Set working directory
WORKDIR /app

# Copy the jar file created using mvn clean install to working directory
COPY target/swiss-dg-db-0.0.1-SNAPSHOT.jar app.jar

# Expose the port
EXPOSE 8080

CMD ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]

