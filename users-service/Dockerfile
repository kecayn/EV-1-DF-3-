FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -DskipTests -pl users-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/users-service/target/users-service-1.0.0.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
