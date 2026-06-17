# ---- Build Stage ----
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -B package -DskipTests

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="DevSecOps Pipeline <devsecops@company.io>"
LABEL description="Headless E-Commerce REST API Engine"

WORKDIR /app

# Create a non-root user for security hardening
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/headless-ecommerce-api-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
