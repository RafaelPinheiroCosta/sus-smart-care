FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/triage-service-*.jar app.jar
EXPOSE 8083
USER 10001
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
