FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/prehospital-service-*.jar app.jar
EXPOSE 8087
USER 10001
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
