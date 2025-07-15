# © 2025 Valian Technologies SpA. Todos los derechos reservados.
# Prohibido su uso o distribución sin autorización expresa.

FROM amazoncorretto:24
EXPOSE 8080
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-server", "-jar", "app.jar"]