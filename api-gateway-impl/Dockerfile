FROM amazoncorretto:21-al2023-headless
WORKDIR /app
COPY target/api-gateway-impl*.jar /app/app.jar
CMD ["java", "-jar", "app.jar"]