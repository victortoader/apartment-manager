#!/bin/bash
docker compose up -d db
echo "Waiting for PostgreSQL to be ready..."
sleep 3
./gradlew bootRun --args='--spring.profiles.active=postgres'
