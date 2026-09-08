# Multi-stage Dockerfile for Notification Service
# Stage 1: Build
FROM maven:3.8-openjdk-11 AS build

WORKDIR /app

# Copy parent POM and common-lib first for better caching
COPY pom.xml ./
COPY common-lib ./common-lib

# Copy notification-service source
COPY notification-service/pom.xml ./notification-service/
COPY notification-service/src ./notification-service/src

# Build common-lib first
WORKDIR /app/common-lib
RUN mvn clean install -DskipTests

# Build notification-service
WORKDIR /app/notification-service
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM openjdk:11-jre-slim

WORKDIR /app

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

# Install wget for health checks
RUN apt-get update && apt-get install -y wget && rm -rf /var/lib/apt/lists/*

# Copy the JAR from build stage
COPY --from=build /app/notification-service/target/*.jar notification-service.jar

# Change ownership to non-root user
RUN chown spring:spring notification-service.jar

# Switch to non-root user
USER spring:spring

# Expose port (notification-service uses 8084)
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/actuator/health || exit 1

# Labels for metadata
LABEL maintainer="stockxpress-team@example.com" \
      service="notification-service" \
      version="1.0-SNAPSHOT" \
      description="Notification service for StockXpress e-commerce platform"

# Run the application
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "notification-service.jar"]