@echo off
echo ==========================================
echo Story Reading Platform - Production Deploy
echo ==========================================
echo.

echo Stopping existing containers...
docker-compose -f docker-compose.prod.yml down

echo.
echo Building and starting all services...
docker-compose -f docker-compose.prod.yml up --build -d

echo.
echo Waiting for services to be healthy...
timeout /t 10 /nobreak > nul

echo.
echo Checking service status...
docker-compose -f docker-compose.prod.yml ps

echo.
echo ==========================================
echo Deployment Complete!
echo ==========================================
echo.
echo Services are running on:
echo   - API Gateway:          http://localhost:8081
echo   - User Service:         http://localhost:8882
echo   - Story Service:        http://localhost:8085
echo   - Payment Service:      http://localhost:8084
echo   - Comment Service:      http://localhost:8883
echo   - Notification Service: http://localhost:8087
echo   - RabbitMQ Management:  http://localhost:15672 (guest/guest)
echo   - PostgreSQL:           localhost:5432 (postgres/postgres123)
echo.
echo To view logs: docker-compose -f docker-compose.prod.yml logs -f [service-name]
echo To stop all:  docker-compose -f docker-compose.prod.yml down
echo.
pause
