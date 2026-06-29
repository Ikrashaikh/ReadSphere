# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Ensure execution permissions for the Maven wrapper
RUN chmod +x mvnw

# Download dependencies to cache them in the Docker layer
RUN ./mvnw dependency:go-offline -B

# Copy source code and package application
COPY src src
RUN ./mvnw package -DskipTests

# Stage 2: Build the runtime container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built jar from Stage 1
COPY --from=build /app/target/BookStore-0.0.1-SNAPSHOT.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
