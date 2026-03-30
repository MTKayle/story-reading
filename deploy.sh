#!/bin/bash

echo "=========================================="
echo "Story Reading Platform - Production Deploy"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Stop and remove existing containers
echo -e "${YELLOW}Stopping existing containers...${NC}"
docker-compose -f docker-compose.prod.yml down

# Remove old images (optional - uncomment if you want to rebuild from scratch)
# echo -e "${YELLOW}Removing old images...${NC}"
# docker-compose -f docker-compose.prod.yml down --rmi all

# Build and start all services
echo -e "${YELLOW}Building and starting all services...${NC}"
docker-compose -f docker-compose.prod.yml up --build -d

# Wait for services to be healthy
echo -e "${YELLOW}Waiting for services to be healthy...${NC}"
sleep 10

# Check service status
echo -e "${GREEN}Checking service status...${NC}"
docker-compose -f docker-compose.prod.yml ps

echo ""
echo -e "${GREEN}=========================================="
echo "Deployment Complete!"
echo "==========================================${NC}"
echo ""
echo "Services are running on:"
echo "  - API Gateway:          http://localhost:8081"
echo "  - User Service:         http://localhost:8882"
echo "  - Story Service:        http://localhost:8085"
echo "  - Payment Service:      http://localhost:8084"
echo "  - Comment Service:      http://localhost:8883"
echo "  - Notification Service: http://localhost:8087"
echo "  - RabbitMQ Management:  http://localhost:15672 (guest/guest)"
echo "  - PostgreSQL:           localhost:5432 (postgres/postgres123)"
echo ""
echo "To view logs: docker-compose -f docker-compose.prod.yml logs -f [service-name]"
echo "To stop all:  docker-compose -f docker-compose.prod.yml down"
echo ""
