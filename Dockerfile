# ---------- Этап 1: Сборка ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Сначала копируем pom.xml и зависимости для кэширования слоев
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходники
COPY src ./src

# Собираем проект. Флаг -DskipTests экономит время.
RUN mvn clean package -DskipTests

# ---------- Этап 2: Запуск ----------
FROM amazoncorretto:17-alpine
WORKDIR /app

LABEL maintainer="Denis Poluyakhtov"

# Копируем собранный JAR со ВСЕМ содержимым (включая /templates внутри!)
COPY --from=builder /build/target/*.jar app.jar

# Устанавливаем curl для healthcheck
RUN apk add --no-cache curl

# HEALTHCHECK теперь проверяет HTTP статус 200
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# ВАЖНО: Убираем volume из compose.yml для ресурсов, если будем использовать этот Dockerfile!
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar app.jar"]