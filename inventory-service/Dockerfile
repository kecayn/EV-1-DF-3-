FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -DskipTests -pl inventory-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/inventory-service/target/inventory-service-1.0.0.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
