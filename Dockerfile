# ================== Stage 1: Frontend Build ==================
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# Copy package files first (layer caching)
COPY frontend/package.json frontend/package-lock.json* ./

# Install dependencies
RUN npm ci --ignore-scripts

# Copy frontend source
COPY frontend/ .

# Build production bundle
RUN npm run build

# ================== Stage 2: Backend Build ==================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and POM first (layer caching for dependencies)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Fix line endings (Windows → Unix) and make wrapper executable
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Copy frontend build output into Spring Boot static resources
COPY --from=frontend-builder /app/frontend/dist/ src/main/resources/static/

# Build (skip tests — they run in CI with Testcontainers)
RUN ./mvnw package -DskipTests -B

# ================== Stage 3: Runtime ==================
FROM eclipse-temurin:21-jre-alpine

# Security: run as non-root
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

WORKDIR /app

# Copy the fat JAR from build stage
COPY --from=builder /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

EXPOSE 8080

# JVM flags: ZGC for low-latency, container-aware memory
ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
