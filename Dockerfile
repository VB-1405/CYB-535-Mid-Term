# Switch to a multi-architecture base image for Mac/Linux compatibility
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/vrishabh-midterm-cicd-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
