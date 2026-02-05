# Stage 1: Build
FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

# Copy Maven wrapper & pom for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the jar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built jar from the previous stage (any JAR ending with SNAPSHOT.jar)
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar

# Set timezone to IST
ENV TZ=Asia/Kolkata
# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
