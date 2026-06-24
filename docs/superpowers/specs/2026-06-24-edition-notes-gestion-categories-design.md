# Édition des notes & gestion des catégories — Design

Date : 2026-06-24
Projet : GestoNote (Spring Boot 4 + Thymeleaf + PostgreSQL)

## 1. Objectif

Finaliser deux fonctionnalités de l'application :

1. **Édition et création de notes** — en mode édition, cliquer sur une note ouvre un
   formulaire permettant de modifier son **titre**, sa **couleur** et son **contenu** ;
   le bouton « Ajouter une note » (aujourd'hui cassé) permet de **créer** une note. Le
   formulaire d'édition existe (`edition.html`) mais est un stub sans handler de
   sauvegarde.
2. **Gestion des catégories** — la page `/categories` (aujourd'hui un simple titre)
   devient une page de gestion permettant d'**activer/désactiver** une catégorie et de
   **renommer** les catégories modifiables.

Rappel du comportement métier : une note sert à **récupérer du texte** (clic =
copier dans le presse-papier en mode normal). La catégorie principale est **non
modifiable** (`estModifiable = false`) : toujours active, non renommable.

## 2. Périmètre

**Inclus**
- Formulaire d'édition de note fonctionnel (titre, couleur, contenu) + sauvegarde.
- Création de note via le bouton « Ajouter une note » (réparation de l'URL).
- En mode édition, le clic sur une note ouvre le formulaire de modification.
- Page de gestion des catégories : activer/désactiver + renommer (modifiables).

**Hors périmètre (YAGNI)**
- Pas de champ couleur sur la catégorie (la couleur ne vit que sur les notes).
- Pas de création ni suppression de catégorie (ensemble fixe issu des fixtures).
- Pas d'édition inline ni d'AJAX : pages Thymeleaf classiques (POST → redirection).

## 3. Modèle de données

Aucune migration de base. Les entités restent inchangées :

- `Note` : `id`, `categorieId`, `titre`, `couleur` (code Integer 1-4), `contenu`.
- `Categorie` : `id`, `denomination`, `estActive`, `estModifiable`.

Le code couleur est porté par l'énumération `CouleurNote`, **étendue de 4 à 8
couleurs** (codes 1 à 8), mappées sur les 8 classes de bouton contextuelles Bootstrap
(aucun CSS custom) :

| Code | Enum | Classe Bootstrap | Teinte |
|------|------|------------------|--------|
| 1 | `PRIMAIRE` | `btn btn-primary` | bleu |
| 2 | `SUCCES` | `btn btn-success` | vert |
| 3 | `AVERTISSEMENT` | `btn btn-warning` | jaune |
| 4 | `DANGER` | `btn btn-danger` | rouge |
| 5 | `SECONDAIRE` | `btn btn-secondary` | gris |
| 6 | `INFO` | `btn btn-info` | cyan |
| 7 | `CLAIR` | `btn btn-light` | clair |
| 8 | `FONCE` | `btn btn-dark` | foncé |

L'énumération est réutilisée pour le sélecteur de couleur et l'affichage. Un code
inconnu ou nul retombe sur la couleur par défaut (`btn btn-primary`).

## 4. Édition et création de note

### 4.1 Liaison de formulaire

Le formulaire `edition.html` lie un **`NoteDto`** (et non `NoteHtml`) : `NoteDto`
porte la couleur sous forme de **code Integer**, ce qui convient au sélecteur radio.
Effet de bord positif : `NoteDto.fromModel(...)`, aujourd'hui inutilisé, devient
utilisé (suppression d'un code mort).

L'identifiant de la note (en modification) transite par l'**URL**, pas par le DTO :
`NoteDto` n'a donc pas besoin de champ `id`.

### 4.2 Contrôleur (`NotePageController`)

| Méthode | Route | Rôle |
|---------|-------|------|
| GET  | `/notes/add/{categorieId}` | Formulaire vierge, `NoteDto` avec `categorieId` pré-rempli |
| GET  | `/notes/update/{id}` | Formulaire pré-rempli via `NoteDto.fromModel(note)` |
| POST | `/notes/add/{categorieId}` | Création → `noteService.addNote(dto)` |
| POST | `/notes/update/{id}` | Modification → `noteService.updateNote(id, dto)` |

Les deux POST appliquent le motif **POST-Redirect-GET** : redirection vers
`/notes?modeEdit=true` après succès. Pour la création, `categorieId` est posé côté
serveur depuis le chemin avant l'appel service.

### 4.3 Vue `notes.html`

- Réparer le lien « Ajouter une note » : `@{/notes/add/{id}(id=${categorie.id})}`
  (actuellement `@{/notes/{id}}`, qui renvoie un 404).
- En **mode édition**, le bouton de note devient un **lien** vers
  `/notes/update/{id}` (ouvre le formulaire). En **mode normal**, comportement
  actuel inchangé : `navigator.clipboard.writeText(...)` (copie du contenu).

### 4.4 Vue `edition.html`

Formulaire avec :
- **Titre** : `input` texte (requis).
- **Couleur** : 8 boutons radio teintés selon `CouleurNote` (codes 1 à 8).
- **Contenu** : `textarea` (requis).
- Boutons **Enregistrer** (submit) et **Annuler** (retour `/notes?modeEdit=true`).
- Le `th:action` pointe vers `/notes/add/{categorieId}` ou `/notes/update/{id}`
  selon le contexte (création vs modification) ; le token CSRF est injecté par
  Thymeleaf.

### 4.5 Validation

- `@NotBlank` sur **titre** et **contenu** du `NoteDto`.
- **couleur** : valeur attendue 1-8 (valeur par défaut 1).
- `categorieId` **ne porte pas** `@NotNull` : il est rempli côté serveur depuis le
  chemin, donc `@Valid` s'exécuterait avant qu'il soit posé (piège de validation des
  conventions maison). La garde anti-null reste dans le **service**.
- En cas d'erreur de validation, le formulaire est ré-affiché avec les messages.

## 5. Gestion des catégories

### 5.1 Service (`CategorieService`)

- **Nouveau** `getAllCategories()` : `findAll` trié par identifiant (actives **et**
  inactives) pour alimenter la page de gestion.
- **Nouveau** `toggleActive(Integer id)` : bascule `estActive`.
- `updateCategorie(id, dto)` **existant** : renomme, lève `IllegalArgumentException`
  si la catégorie n'est pas modifiable — réutilisé tel quel.
- Le `categorieTrigger(id)` existant bascule `estModifiable` (verrou de renommage),
  comportement **non désiré** ici : il devient du code mort et est **supprimé**
  (conformément à la règle « pas de code mort »). `getAllCategoriesModifiables()`, si
  inutilisé après coup, est également retiré.

Tri : le service appelle `categorieRepository.findAll(Sort.by(Sort.Direction.ASC,
"id"))` — pas de nouvelle méthode de dépôt nécessaire.

### 5.2 Contrôleur (`CategoriePageController`)

| Méthode | Route | Rôle |
|---------|-------|------|
| GET  | `/categories` | Liste toutes les catégories (gestion) |
| POST | `/categories/{id}/toggle-active` | Bascule actif/inactif |
| POST | `/categories/{id}/rename` | Renomme (CategorieDto `denomination`) |

Les POST appliquent POST-Redirect-GET : redirection vers `/categories`.

### 5.3 Vue `categories.html`

Tableau des catégories, une ligne par catégorie :
- **Titre** : pour une catégorie modifiable, `input` + bouton « Renommer » (POST
  rename) ; pour la **principale** (non modifiable), texte simple en lecture seule.
- **État** : badge « Active » / « Inactive ».
- **Action** : bouton bascule (POST toggle-active).

La catégorie principale reste toujours présente ; son champ de renommage est
désactivé côté vue, et le service refuse de toute façon le renommage d'une catégorie
non modifiable (double garde).

## 6. Sécurité

Aucune modification de `SecurityConfiguration`. Les routes `/notes/**` et
`/categories` (et ses sous-chemins) sont déjà `authenticated()`. Le CSRF est actif ;
les formulaires Thymeleaf injectent automatiquement le token `_csrf` dans les POST.

## 7. Tests (couverture ≥ 90 %, conventions maison)

- **Service (Mockito)** : `getAllCategories`, `toggleActive`, `updateCategorie`
  (modifiable / non modifiable), `addNote`, `updateNote` (déjà couverts, complétés au
  besoin).
- **Contrôleurs (`@WebMvcTest`, sécurité importée)** :
  - GET/POST `/notes/add/{id}` et `/notes/update/{id}` (avec utilisateur authentifié
    et token CSRF) ; vérifier la redirection et l'appel service.
  - POST `/categories/{id}/toggle-active` et `/categories/{id}/rename` ; GET
    `/categories` affiche toutes les catégories.
- `./mvnw verify` doit rester **vert** (Checkstyle 0 violation + JaCoCo ≥ 90 %).
- `./mvnw clean verify site` doit continuer à générer le site sans erreur.

## 8. Impacts sur le code existant

- `pom.xml` : ajout de **`spring-boot-starter-validation`** (absent du classpath) pour
  activer la validation Bean (`@NotBlank`, `@Valid`).
- `model/CouleurNote.java` : **extension de 4 à 8 couleurs** (ajout de `SECONDAIRE`,
  `INFO`, `CLAIR`, `FONCE` — codes 5 à 8) ; test `CouleurNoteTest` mis à jour.
- `NotePageController` : les GET add/update construisent désormais un `NoteDto`
  (au lieu d'un `NoteHtml`) ; ajout des deux POST.
- `NoteDto` : ajout des contraintes `@NotBlank` (titre, contenu).
- `CategorieService` : ajout de `getAllCategories` et `toggleActive` ; suppression du
  `categorieTrigger` mort (et de `getAllCategoriesModifiables` si inutilisé).
- `CategoriePageController` : ajout des handlers ; injection de `CategorieService`.
- Vues : `notes.html` (lien add + clic édition), `edition.html` (formulaire complet),
  `categories.html` (tableau de gestion).
- `NoteHtml` reste utilisé pour l'**affichage** du tableau de notes (couleur = classe
  CSS) ; il n'est plus utilisé par le formulaire d'édition.

## 9. Livraison

Gros chantier → **branche dédiée** (`feat/edition-notes-categories`), commits français
explicites et fréquents, build vert avant chaque commit, finalisation par merge ou PR
selon le choix de l'utilisateur.
