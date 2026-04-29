FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -DskipTests -pl shipment-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/shipment-service/target/shipment-service-1.0.0.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
