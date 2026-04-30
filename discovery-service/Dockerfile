FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY . .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -DskipTests -pl discovery-service -am package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/discovery-service/target/discovery-service-1.0.0.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
