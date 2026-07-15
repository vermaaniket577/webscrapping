# Stage 1: Build the Spring Boot application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies
RUN mvn dependency:go-offline -B
COPY src ./src
# Build the executable JAR
RUN mvn clean package -DskipTests

# Stage 2: Production runtime image with OpenJDK 17 and native Tesseract OCR
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install native Tesseract OCR and English language packs
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from stage 1
COPY --from=build /app/target/*.jar app.jar

# Configure dynamic environment variables for Spring Boot and OCR paths in Linux
ENV PORT=8080
ENV OCR_TESSDATA_PATH=/usr/share/tesseract-ocr/5/tessdata
ENV OCR_NATIVE_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu
ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE ${PORT}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Docr.tessdataPath=${OCR_TESSDATA_PATH} -Docr.nativeLibraryPath=${OCR_NATIVE_LIBRARY_PATH} -jar app.jar"]
