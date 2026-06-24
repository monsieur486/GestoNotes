# GestoNote

GestoNote est une application web de gestion de notes classées par catégories, avec
authentification. Les notes s'affichent dans un tableau par catégorie et peuvent être
colorées, créées, modifiées ou supprimées.

Stack : Java 17, Spring Boot 4, Spring Security, Spring Data JPA, Thymeleaf,
PostgreSQL (migrations Liquibase), build Maven.

## Prérequis

- Java 17 (JDK)
- Maven via le wrapper `./mvnw` (rien à installer)
- Docker + Docker Compose (pour la base PostgreSQL)
- Un fichier `.env` à la racine, copié depuis le modèle : `cp dist.env .env`

## Démarrage en développement

En développement, seule la base tourne dans Docker ; l'application Java est lancée
en local pour itérer rapidement.

```bash
cp dist.env .env        # crée la config locale (puis éditer .env si besoin)
./dev-start.sh          # démarre la base PostgreSQL (Docker)
./mvnw spring-boot:run  # lance l'application sur http://localhost:8080
./dev-stop.sh           # arrête la base
```

L'application est accessible sur `http://localhost:8080`. La page de connexion est
`/login` ; les identifiants proviennent des variables `APP_USERNAME` / `APP_PASSWORD`
du fichier `.env`.

## Build & tests

```bash
./mvnw clean verify        # build + tests + Checkstyle (bloquant) + couverture JaCoCo (>= 90%)
./mvnw clean verify site   # idem + site Maven (Javadoc FR, JaCoCo, Surefire) dans target/site/
```

Le site généré (`target/site/index.html`) regroupe les informations du projet, la
Javadoc en français, le tableau de couverture JaCoCo et le rapport des tests Surefire.

## Déploiement (pile complète conteneurisée)

En production, l'application et la base tournent toutes deux dans Docker
(profil `fullstack`).

```bash
./prod-start.sh   # construit et démarre la pile complète (base + application)
./prod-stop.sh    # arrête la pile complète
./maj.sh          # mise à jour : arrêt, git pull, reconstruction et redémarrage
```

## Configuration

Toutes les valeurs sensibles ou variables d'environnement sont externalisées dans
`.env` (ignoré par git). Le fichier `dist.env`, versionné, sert de modèle : il ne
contient **aucun secret réel**, ses mots de passe valent `a_changer_…` et **doivent**
être remplacés par des secrets forts dans le `.env` local (jamais en production avec
les valeurs du modèle).

| Variable            | Rôle                                              |
|---------------------|---------------------------------------------------|
| `POSTGRES_DB`       | Nom de la base PostgreSQL                          |
| `POSTGRES_USER`     | Utilisateur de la base                            |
| `POSTGRES_PASSWORD` | Mot de passe de la base                           |
| `APP_USERNAME`      | Identifiant de connexion à l'application          |
| `APP_PASSWORD`      | Mot de passe de connexion à l'application         |
| `DOCKER_UI_PORT`    | Port d'écoute HTTP de l'application               |
