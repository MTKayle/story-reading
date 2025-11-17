-- Tạo database cho favourite-service
CREATE DATABASE favourite_db;

-- Kết nối vào database
\c favourite_db

-- Các bảng sẽ được tự động tạo bởi Hibernate khi service khởi động
-- Với cấu hình spring.jpa.hibernate.ddl-auto=update

