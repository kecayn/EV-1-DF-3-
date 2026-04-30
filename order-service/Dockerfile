FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -DskipTests -pl order-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/order-service/target/order-service-1.0.0.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
