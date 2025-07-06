FROM amazoncorretto:24
EXPOSE 8080
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-server", "-jar", "app.jar"]