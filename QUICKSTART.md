# 🚀 Guide de démarrage rapide - Chatbot AI

Ce guide vous permettra de faire fonctionner le chatbot AI en moins de 15 minutes.

## ⚡ Démarrage en 3 étapes

### 1. Prérequis rapides

Assurez-vous d'avoir installé :

- **Java 21** : `java -version`
- **Docker & Docker Compose** : `docker --version && docker-compose --version`
- **Git** : `git --version`

### 2. Configuration minimale

```bash
# 1. Cloner le projet
git clone https://github.com/votre-repo/ai-chatbot.git
cd ai-chatbot

# 2. Créer le fichier de configuration local
cp .env.example .env.local

# 3. Éditer les variables essentielles (minimum requis)
nano .env.local
```

**Variables obligatoires à configurer :**

```bash
AZURE_OPENAI_API_KEY=votre-vraie-cle-ici
AZURE_OPENAI_ENDPOINT=https://votre-instance.openai.azure.com/
AZURE_SEARCH_API_KEY=votre-cle-search-ici
AZURE_SEARCH_ENDPOINT=https://votre-search.search.windows.net
```

### 3. Lancement automatique

```bash
# Démarrage avec configuration automatique
./start-local.sh
```

Le script va automatiquement :

- ✅ Démarrer SQL Server dans Docker
- ✅ Créer la base de données
- ✅ Construire l'application
- ✅ Lancer le chatbot

## 🧪 Test rapide

Une fois l'application démarrée :

### Health Check

```bash
curl http://localhost:8080/api/health
# Réponse attendue: {"status":"UP","service":"AI Chatbot",...}
```

### Test de chat

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "test-123",
    "userId": "user-456",
    "text": "Comment créer une API REST avec Spring Boot ?"
  }'
```

### Interface d'administration

- **Statistiques** : `GET http://localhost:8080/api/admin/stats`
- **Health complet** : `GET http://localhost:8080/api/admin/health`

## 📊 Tableaux de bord

### Actuator (monitoring)

- http://localhost:8080/actuator/health
- http://localhost:8080/actuator/metrics

### Logs en temps réel

```bash
# Suivre les logs de l'application
tail -f logs/application.log

# Logs du conteneur SQL Server
docker-compose logs -f sqlserver
```

## 🔧 Configuration avancée (optionnel)

### Personnaliser le message système

Éditez `application.properties` :

```properties
chatbot.system-message=Vous êtes un expert Spring Boot spécialisé dans les microservices...
```

### Régler les paramètres RAG

```properties
chatbot.rag.max-results=3          # Nb documents récupérés
chatbot.rag.similarity-threshold=0.8  # Seuil de pertinence
```

### Mode débug

```bash
LOGGING_LEVEL_COM_GITHUB_BOISVERTMATHIEU=DEBUG ./gradlew bootRun
```

## 🚨 Résolution des problèmes courants

### SQL Server ne démarre pas

```bash
# Vérifier les conteneurs
docker ps -a

# Redémarrer SQL Server
docker-compose down && docker-compose up -d sqlserver

# Vérifier la connectivité
docker-compose exec sqlserver /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P "DevPassword123!" -Q "SELECT 1"
```

### Erreurs de connexion Azure

```bash
# Vérifier les variables d'environnement
env | grep AZURE

# Tester la connectivité OpenAI
curl -H "api-key: $AZURE_OPENAI_API_KEY" "$AZURE_OPENAI_ENDPOINT/openai/deployments?api-version=2024-02-01"
```

### Port 8080 déjà utilisé

```bash
# Changer le port dans .env.local
echo "SERVER_PORT=8081" >> .env.local

# Ou arrêter le processus existant
sudo lsof -ti:8080 | xargs kill -9
```

## 📁 Structure des données

### Tables créées automatiquement

- `conversations` : Historique des échanges
- `knowledge_documents` : Base de connaissances
- `shedlock` : Verrouillage des tâches planifiées

### Répertoires importants

```
├── logs/              # Logs de l'application
├── docker-compose.yml # Configuration SQL Server
├── .env.local         # Configuration locale
└── build/libs/        # JAR généré
```

## 🎯 Prochaines étapes

1. **Ajouter des documents** à la base de connaissances :

   ```bash
   curl -X POST http://localhost:8080/api/admin/knowledge \
     -F "title=Guide Spring Security" \
     -F "content=Contenu du guide..." \
     -F "tags=spring,security"
   ```

2. **Configurer Teams** : Remplacer les webhooks de test dans `.env.local`

3. **Déployer en production** : Suivre le guide complet dans `README.md`

## 💡 Conseils de développement

### Mode développement continu

```bash
# Rechargement automatique (DevTools activé)
./gradlew bootRun --continuous

# Tests en continu
./gradlew test --continuous
```

### Debug avec IDE

- Point d'entrée : `AiChatbotApplication.java`
- Profil : `dev`
- VM Options : `-Dspring.profiles.active=dev`

### Base de connaissances initiale

Déposez vos fichiers dans `/docs` et exécutez :

```bash
curl -X POST http://localhost:8080/api/admin/index/trigger
```

---

🎉 **Félicitations !** Votre chatbot AI est maintenant opérationnel.

Pour des fonctionnalités avancées, consultez le [README complet](README.md).
