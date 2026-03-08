# Use Eclipse Temurin JRE 17 for a smaller, professional image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/vrishabh-midterm-cicd-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
