@echo off
chcp 65001 >nul
echo === Сборка JAR-файла ===
call mvnw.cmd clean package -DskipTests
if %errorlevel% neq 0 (
    echo Ошибка сборки. Нажми любую клавишу для выхода.
    pause
    exit /b %errorlevel%
)

echo === Запуск Docker Compose ===
docker-compose up --build

pause