FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk
RUN groupadd -r spring && useradd -r -g spring spring
EXPOSE 8080
USER spring:spring
ARG JAR_FILE=/app/target/*.jar
COPY --from=builder ${JAR_FILE} /app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
