#!/bin/bash

# Script de démarrage local pour le chatbot AI
# Ce script configure l'environnement local avec Docker Compose

set -e

echo "🚀 Démarrage du Chatbot AI en mode développement local"

# Vérifier si Docker est installé
if ! command -v docker &> /dev/null; then
    echo "❌ Docker n'est pas installé. Veuillez l'installer d'abord."
    exit 1
fi

# Vérifier si Docker Compose est installé
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose n'est pas installé. Veuillez l'installer d'abord."
    exit 1
fi

# Créer le fichier docker-compose.yml pour SQL Server
echo "📦 Configuration de SQL Server avec Docker..."
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  sqlserver:
    image: mcr.microsoft.com/mssql/server:2022-latest
    container_name: ai-chatbot-sqlserver
    environment:
      - ACCEPT_EULA=Y
      - SA_PASSWORD=DevPassword123!
      - MSSQL_PID=Developer
    ports:
      - "1433:1433"
    volumes:
      - sqlserver_data:/var/opt/mssql
    healthcheck:
      test: /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P DevPassword123! -Q "SELECT 1"
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

volumes:
  sqlserver_data:
EOF

# Démarrer SQL Server
echo "🔄 Démarrage de SQL Server..."
docker-compose up -d sqlserver

# Attendre que SQL Server soit prêt
echo "⏳ Attente que SQL Server soit prêt..."
timeout=60
while [ $timeout -gt 0 ]; do
    if docker-compose exec -T sqlserver /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "DevPassword123!" -Q "SELECT 1" &> /dev/null; then
        echo "✅ SQL Server est prêt!"
        break
    fi
    echo "   Attente... ($timeout secondes restantes)"
    sleep 2
    timeout=$((timeout-2))
done

if [ $timeout -le 0 ]; then
    echo "❌ Timeout: SQL Server n'a pas démarré à temps"
    exit 1
fi

# Créer le fichier .env.local s'il n'existe pas
if [ ! -f .env.local ]; then
    echo "📝 Création du fichier .env.local..."
    cat > .env.local << 'EOF'
# Configuration locale pour le développement
AZURE_OPENAI_API_KEY=your-api-key-here
AZURE_OPENAI_ENDPOINT=https://your-instance.openai.azure.com/
AZURE_OPENAI_CHAT_DEPLOYMENT=gpt-4o
AZURE_OPENAI_EMBEDDING_DEPLOYMENT=text-embedding-3-small

AZURE_SEARCH_API_KEY=your-search-api-key-here
AZURE_SEARCH_ENDPOINT=https://your-search-service.search.windows.net
AZURE_SEARCH_INDEX_NAME=chatbot-knowledge-base-dev

AZURE_SQL_URL=jdbc:sqlserver://localhost:1433;databaseName=aichatbot;encrypt=false;trustServerCertificate=true
AZURE_SQL_USERNAME=sa
AZURE_SQL_PASSWORD=DevPassword123!

TEAMS_TEST_WEBHOOK=https://your-test-webhook-here
TEAMS_PROD_WEBHOOK=https://your-prod-webhook-here
TEAMS_MODE=test

SPRING_PROFILES_ACTIVE=dev
EOF

    echo "⚠️  IMPORTANT: Veuillez éditer le fichier .env.local avec vos vraies clés Azure!"
    echo "   Fichier créé: .env.local"
fi

# Charger les variables d'environnement
if [ -f .env.local ]; then
    echo "📂 Chargement des variables d'environnement depuis .env.local"
    export $(grep -v '^#' .env.local | xargs)
fi

# Construire l'application
echo "🔨 Construction de l'application..."
./gradlew build -x test

# Démarrer l'application
echo "🚀 Démarrage de l'application Spring Boot..."
echo "   L'application sera accessible sur: http://localhost:8080"
echo "   Health check: http://localhost:8080/actuator/health"
echo "   API Documentation: http://localhost:8080/api"
echo ""
echo "   Pour arrêter l'application: Ctrl+C"
echo "   Pour arrêter SQL Server: docker-compose down"
echo ""

# Démarrer avec le profil de développement
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun 