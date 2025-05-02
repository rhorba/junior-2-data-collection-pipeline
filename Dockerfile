FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/data-pipeline-*.jar app.jar

# Enable Spring DevTools for hot reloading
ENV SPRING_DEVTOOLS_RESTART_ENABLED=true

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
