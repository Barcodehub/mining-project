# Dockerfile para mining-audit-notifications
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY build/libs/power-up-arquetipo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
