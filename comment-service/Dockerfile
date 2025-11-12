# Sử dụng Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk

# Tạo thư mục app trong container
WORKDIR /app

# Copy file jar đã build vào container và đổi tên thành app.jar
COPY target/comment-service-0.0.1-SNAPSHOT.jar app.jar

# Mở port service
EXPOSE 8883

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]