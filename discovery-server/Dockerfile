# Multi-stage build for Discovery Server (Eureka)
# Build stage
FROM maven:3.8-openjdk-11 AS build
WORKDIR /app

# Copy parent pom first for better layer caching
COPY ../pom.xml ../pom.xml
COPY ../common-lib ../common-lib

# Copy service pom and source
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:11-jre-slim

# Add metadata labels
LABEL maintainer="stockxpress"
LABEL service="discovery-server"
LABEL version="1.0"
LABEL description="Eureka Discovery Server for StockXpress Microservices"

# Create non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring

# Set working directory
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose Eureka port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8761/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]