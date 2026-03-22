# ─────────────────────────────────────────────────────────────────────────────
# Multi-Stage Dockerfile for User Service
# ─────────────────────────────────────────────────────────────────────────────
#
# WHY MULTI-STAGE BUILDS?
# ──────────────────────────
# Stage 1 (builder): Contains Maven + JDK to compile the application
# Stage 2 (runtime): Contains ONLY the JRE + final JAR
#
# Benefits:
# - Final image is much smaller (~200MB instead of ~600MB)
# - Maven, source code, and build tools are NOT in the production image
# - Smaller image = faster pulls = lower AWS ECR storage + transfer costs
#
# AWS FREE TIER RELEVANCE:
# ─────────────────────────
# - ECR (Elastic Container Registry): 500MB/month free storage
#   A smaller image fits within free tier more easily.
# - ECS Fargate: billed per vCPU/memory-second
#   Smaller image starts faster → less startup time billed.
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: BUILD ───────────────────────────────────────────────────────────
# Use the official Maven image with Amazon Corretto 17 (Amazon's JDK — optimized for AWS)
FROM maven:3.9.6-amazoncorretto-17 AS builder

# Set working directory inside the container
WORKDIR /app

# OPTIMIZATION: Copy pom.xml first and download dependencies separately.
# Docker caches each layer. If pom.xml doesn't change, Maven doesn't re-download
# all dependencies on every build — makes subsequent builds much faster.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Now copy source code and build
COPY src ./src

# Package the application into a single executable JAR
# -DskipTests: skip tests during Docker build (tests run in CI pipeline separately)
RUN mvn package -DskipTests -B

# ── Stage 2: RUNTIME ─────────────────────────────────────────────────────────
# Use Amazon Corretto 17 JRE (run-only, smaller than JDK)
FROM amazoncorretto:17-alpine

# Install curl for Docker/ECS health checks
RUN apk add --no-cache curl

# Security best practice: don't run as root
# Create a dedicated non-root user for the application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy ONLY the compiled JAR from the build stage
# Nothing else from stage 1 comes into this image
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose the port this service listens on
EXPOSE 8001

# JVM flags for container environments:
# -XX:+UseContainerSupport    → JVM respects Docker CPU/memory limits (Java 10+)
# -XX:MaxRAMPercentage=75.0   → Use at most 75% of container RAM for JVM heap
# These prevent the JVM from thinking it has access to ALL host memory
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
