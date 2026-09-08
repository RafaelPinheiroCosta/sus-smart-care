FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/api-gateway-*.jar app.jar
EXPOSE 8080
USER 10001
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
