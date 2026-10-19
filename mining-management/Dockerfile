# Dockerfile para mining-management
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY build/libs/power-up-arquetipo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
