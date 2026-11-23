# 1) Angular
FROM node:24-alpine AS front
WORKDIR /front
COPY viaveneto-frontend/package*.json ./
RUN npm ci
COPY viaveneto-frontend/ ./
RUN npm run build -- --configuration production

# 2) Spring Boot con el front adentro de /static
FROM maven:3.9-eclipse-temurin-21 AS back
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline -B
COPY src ./src
COPY --from=front /front/dist/viaveneto-frontend/browser/ ./src/main/resources/static/
RUN mvn -q clean package -DskipTests

# 3) Imagen final
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=back /app/target/*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-Duser.timezone=America/Argentina/Buenos_Aires", "-jar", "app.jar"]
