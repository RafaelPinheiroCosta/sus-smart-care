FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/telemetry-service-*.jar app.jar
EXPOSE 8085
USER 10001
ENTRYPOINT ["java","-XX:InitialRAMPercentage=10","-XX:MaxRAMPercentage=45","-XX:+ExitOnOutOfMemoryError","-jar","/app/app.jar"]
