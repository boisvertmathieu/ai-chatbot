# Chatbot AI avec Spring Boot et Azure OpenAI

Ce projet implémente un chatbot intelligent utilisant Spring Boot 3.5, Azure OpenAI et Azure AI Search pour répondre automatiquement aux questions de support récurrentes.

## 🚀 Fonctionnalités

- **RAG (Retrieval-Augmented Generation)** : Recherche contextuelle dans une base de connaissances
- **Azure OpenAI GPT-4o** : Génération de réponses intelligentes
- **Azure AI Search** : Indexation vectorielle des documents
- **Azure SQL Database** : Persistance des conversations et feedbacks
- **Teams Integration** : Notifications vers canaux Teams (test/production)
- **Apprentissage continu** : Indexation automatique des réponses corrigées
- **Tâches planifiées** : Synchronisation nocturne avec ShedLock
- **API REST** : Endpoints pour chat, feedback et administration

## 📋 Prérequis

- Java 21 ou supérieur
- Gradle 8.14
- Azure OpenAI (GPT-4o + text-embedding-3-small)
- Azure AI Search avec support vectoriel
- Azure SQL Database
- Comptes Teams avec webhooks configurés

## ⚙️ Configuration

### Variables d'environnement

Créez un fichier `.env` ou configurez les variables suivantes :

```bash
# Azure OpenAI
AZURE_OPENAI_API_KEY=votre-cle-api
AZURE_OPENAI_ENDPOINT=https://votre-instance.openai.azure.com/
AZURE_OPENAI_CHAT_DEPLOYMENT=gpt-4o
AZURE_OPENAI_EMBEDDING_DEPLOYMENT=text-embedding-3-small

# Azure AI Search
AZURE_SEARCH_API_KEY=votre-cle-search
AZURE_SEARCH_ENDPOINT=https://votre-search-service.search.windows.net
AZURE_SEARCH_INDEX_NAME=chatbot-knowledge-base

# Azure SQL Database
AZURE_SQL_URL=jdbc:sqlserver://votre-serveur.database.windows.net:1433;databaseName=aichatbot;encrypt=true
AZURE_SQL_USERNAME=votre-username
AZURE_SQL_PASSWORD=votre-password

# Teams Webhooks
TEAMS_TEST_WEBHOOK=https://votre-webhook-teams-test
TEAMS_PROD_WEBHOOK=https://votre-webhook-teams-prod
TEAMS_MODE=test
```

### Configuration Azure

1. **Azure OpenAI** :

   - Créez une ressource Azure OpenAI
   - Déployez les modèles GPT-4o et text-embedding-3-small
   - Notez l'endpoint et la clé API

2. **Azure AI Search** :

   - Créez un service Azure AI Search
   - Activez les fonctionnalités vectorielles
   - Notez l'endpoint et la clé admin

3. **Azure SQL Database** :
   - Créez une base de données Azure SQL
   - Configurez les règles de pare-feu
   - Les tables seront créées automatiquement par Hibernate

## 🏗️ Installation et déploiement

### Développement local

```bash
# 1. Cloner le projet
git clone https://github.com/votre-repo/ai-chatbot.git
cd ai-chatbot

# 2. Configurer les variables d'environnement
cp .env.example .env
# Éditer .env avec vos valeurs

# 3. Construire et lancer
./gradlew bootRun
```

L'application sera accessible sur `http://localhost:8080`

### Déploiement sur Azure

#### Option 1: Azure Container Instances

```bash
# 1. Construire l'image Docker
./gradlew bootBuildImage

# 2. Pousser vers Azure Container Registry
az acr login --name votre-registry
docker tag ai-chatbot:latest votre-registry.azurecr.io/ai-chatbot:latest
docker push votre-registry.azurecr.io/ai-chatbot:latest

# 3. Déployer sur ACI
az container create \
  --resource-group votre-rg \
  --name ai-chatbot \
  --image votre-registry.azurecr.io/ai-chatbot:latest \
  --environment-variables $(cat .env)
```

#### Option 2: Azure App Service

```bash
# 1. Créer l'App Service
az webapp create \
  --resource-group votre-rg \
  --plan votre-plan \
  --name votre-chatbot-app \
  --deployment-container-image-name votre-registry.azurecr.io/ai-chatbot:latest

# 2. Configurer les variables d'environnement
az webapp config appsettings set \
  --resource-group votre-rg \
  --name votre-chatbot-app \
  --settings @appsettings.json
```

## 🔌 Utilisation des APIs

### Chat endpoint

```bash
POST /api/chat
Content-Type: application/json

{
  "conversationId": "conv-123",
  "userId": "user-456",
  "text": "Comment configurer Spring Security ?"
}
```

Réponse :

```json
{
  "conversationId": "conv-123",
  "response": "Pour configurer Spring Security...",
  "retrievedDocumentIds": ["doc1", "doc2"],
  "tokensUsed": 150,
  "timestamp": "2024-01-15T10:30:00",
  "success": true
}
```

### Feedback endpoint

```bash
POST /api/feedback
Content-Type: application/json

{
  "conversationId": "conv-123",
  "useful": true,
  "correctedResponse": "Réponse corrigée optionnelle..."
}
```

### Administration

```bash
# Statistiques
GET /api/admin/stats

# Ajouter un document à la base de connaissances
POST /api/admin/knowledge
Content-Type: application/x-www-form-urlencoded

title=Guide Spring Boot&content=Contenu du guide...&source=manual&tags=spring,boot

# Déclencher l'indexation manuelle
POST /api/admin/index/trigger

# Health check
GET /api/admin/health
```

## 📊 Monitoring et maintenance

### Logs

Les logs sont configurés avec Logback et incluent :

- Requêtes et réponses du chatbot
- Erreurs et exceptions détaillées
- Métriques de performance Azure OpenAI
- Statut des tâches planifiées

### Métriques

Utilisez Spring Boot Actuator :

- `/actuator/health` : Santé de l'application
- `/actuator/metrics` : Métriques détaillées
- `/actuator/info` : Informations sur l'application

### Tâches planifiées

- **Indexation des réponses corrigées** : Tous les jours à 2h00
- **Synchronisation Azure AI Search** : Toutes les heures
- Verrous distribués avec ShedLock pour éviter les doublons

## 🔧 Configuration avancée

### Personnalisation du message système

Modifiez `chatbot.system-message` dans `application.properties` :

```properties
chatbot.system-message=Vous êtes un expert en développement Spring Boot. Répondez de manière précise et professionnelle en français. Utilisez le contexte fourni pour enrichir vos réponses.
```

### Réglage des paramètres RAG

```properties
# Nombre maximum de documents récupérés
chatbot.rag.max-results=5

# Seuil de similarité vectorielle (0.0 à 1.0)
chatbot.rag.similarity-threshold=0.7
```

### Paramètres Azure OpenAI

```properties
# Température (créativité des réponses)
spring.ai.azure.openai.chat.options.temperature=0.7

# Nombre maximum de tokens
spring.ai.azure.openai.chat.options.max-tokens=1000
```

## 🚀 Pipeline CI/CD

Le projet inclut des workflows GitHub Actions pour :

1. **Tests automatisés** : Tests unitaires et d'intégration
2. **Build et packaging** : Construction de l'image Docker
3. **Déploiement** : Déploiement automatique sur Azure
4. **Sécurité** : Scan des vulnérabilités

### Configuration du pipeline

1. Configurez les secrets GitHub :

   - `AZURE_CREDENTIALS`
   - `AZURE_REGISTRY_LOGIN_SERVER`
   - `AZURE_REGISTRY_USERNAME`
   - `AZURE_REGISTRY_PASSWORD`

2. Adaptez `.github/workflows/deploy.yml` à votre environnement

## 🧪 Tests

```bash
# Tests unitaires
./gradlew test

# Tests d'intégration
./gradlew integrationTest

# Tests avec couverture
./gradlew jacocoTestReport
```

## 📝 Structure du projet

```
src/
├── main/java/com/github/boisvertmathieu/aichatbot/
│   ├── controller/          # Contrôleurs REST
│   ├── service/             # Logique métier
│   ├── repository/          # Accès aux données
│   ├── entity/              # Entités JPA
│   ├── dto/                 # Objets de transfert
│   └── config/              # Configuration Spring
└── main/resources/
    ├── application.properties
    └── application-prod.properties
```

## 🤝 Contribution

1. Fork le projet
2. Créez une branche feature (`git checkout -b feature/nouvelle-fonctionnalite`)
3. Committez vos changements (`git commit -am 'Ajout nouvelle fonctionnalité'`)
4. Poussez la branche (`git push origin feature/nouvelle-fonctionnalite`)
5. Créez une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 🔗 Liens utiles

- [Documentation Spring AI](https://docs.spring.io/spring-ai/reference/)
- [Azure OpenAI Documentation](https://docs.microsoft.com/en-us/azure/cognitive-services/openai/)
- [Azure AI Search](https://docs.microsoft.com/en-us/azure/search/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)

## 📞 Support

Pour toute question ou problème :

- Créez une issue sur GitHub
- Consultez la documentation
- Contactez l'équipe de développement
