FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/facility-service-*.jar app.jar
EXPOSE 8091
USER 10001
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
