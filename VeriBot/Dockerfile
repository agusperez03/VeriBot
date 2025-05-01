FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/veribot-1.0-SNAPSHOT.jar /app/veribot.jar
COPY .env /app/.env
EXPOSE 8080
CMD ["java", "-jar", "veribot.jar"]

