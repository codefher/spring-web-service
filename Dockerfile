# 1) Build: usa Maven+JDK para compilar y empaquetar
FROM maven:3.8.8-openjdk-17 AS build

WORKDIR /app

# Copia solo lo necesario para cache de dependencias
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Copia código fuente
COPY src src

# Dale permiso al wrapper y lanza la build
RUN chmod +x mvnw \
  && ./mvnw clean package -DskipTests

# 2) Runtime: empaqueta el JAR en una imagen ligera de Java
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copia el JAR generado por la etapa build
COPY --from=build /app/target/*.jar app.jar

# Expón el puerto en el que tu Spring arranca (cualquiera que definas en application.properties)
EXPOSE 8080

# Punto de entrada
ENTRYPOINT ["java","-jar","/app/app.jar"]
