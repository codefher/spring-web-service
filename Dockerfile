FROM maven:3.8.8-eclipse-temurin-17-alpine AS build

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw \
  && ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

# Instala curl en la imagen de producción
RUN apk add --no-cache curl

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
