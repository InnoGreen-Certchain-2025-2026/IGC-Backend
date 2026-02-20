# IGC-Backend

Backend cho hệ thống chứng chỉ số trên blockchain (IGC). Dự án sử dụng Spring Boot, PostgreSQL, Web3j, AWS S3,
JWT/OAuth2 Resource Server và Spring AI (OpenAI).

**Stack chính**

- Java 21, Spring Boot 4
- Spring MVC, Spring Security (JWT + OAuth2 Resource Server)
- Spring Data JPA + PostgreSQL
- Web3j (tương tác blockchain)
- AWS S3 (lưu trữ file)
- Springdoc OpenAPI (Swagger UI)
- Spring AI (OpenAI chat)

**Yêu cầu**

- JDK 21
- PostgreSQL 14+
- Node/Blockchain RPC (nếu dùng tính năng blockchain)
- AWS S3 credentials (nếu dùng upload)
- OpenAI API key (nếu dùng AI)

**Cấu hình**
Ứng dụng dùng các biến môi trường sau (xem `src/main/resources/application.yml`):

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `OPENAI_API_KEY`
- `OPENAI_CHAT_MODEL`
- `BLOCKCHAIN_CONTRACT_ADDRESS`
- `BLOCKCHAIN_ADMIN_PRIVATE_KEY`
- `SECURITY_CORS_ALLOWED_ORIGINS`
- `SECURITY_JWT_SECRET`
- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `AWS_BUCKET_NAME`
- `AWS_DOMAIN`
- `AWS_REGION`

Ví dụ `.env` (chỉ để tham khảo):

```
DB_URL=jdbc:postgresql://localhost:5432/igc-db
DB_USERNAME=postgres
DB_PASSWORD=your_password
OPENAI_API_KEY=sk-...
OPENAI_CHAT_MODEL=gpt-5-nano
BLOCKCHAIN_CONTRACT_ADDRESS=0x...
BLOCKCHAIN_ADMIN_PRIVATE_KEY=0x...
SECURITY_CORS_ALLOWED_ORIGINS=http://localhost:4000
SECURITY_JWT_SECRET=your_jwt_secret
AWS_ACCESS_KEY=...
AWS_SECRET_KEY=...
AWS_BUCKET_NAME=...
AWS_DOMAIN=https://<bucket>.s3.<region>.amazonaws.com
AWS_REGION=ap-southeast-1
```

**Chạy dự án**

- Chạy mặc định (đọc biến môi trường trong `application.yml`)

```
./mvnw spring-boot:run
```

- Chạy profile dev

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Chạy profile cloud

```
./mvnw spring-boot:run -Dspring-boot.run.profiles=cloud
```

**Build**

```
./mvnw -DskipTests package
```

**Docker image (Jib)**

```
./mvnw jib:build
```

Ảnh sẽ được đẩy lên theo cấu hình trong `pom.xml` (plugin `jib-maven-plugin`).

**API docs**

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

**Health check**

- `http://localhost:8080/actuator/health`