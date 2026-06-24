# Transfert de catégorie d'une note — Design

Date : 2026-06-24
Projet : GestoNote (Spring Boot 4 + Thymeleaf + PostgreSQL)

## 1. Objectif

Dans le formulaire d'édition de note, **en modification d'une note existante**, permettre de
**transférer la note vers une autre catégorie** via une liste déroulante — uniquement
lorsqu'il existe **au moins 2 catégories actives** (la principale + au moins une secondaire).
Quand une seule catégorie est active (ou en création), aucune liste : il n'y a rien vers quoi
transférer.

## 2. Périmètre

**Inclus**
- Une liste déroulante (`<select>`) des **catégories actives** dans le formulaire de
  **modification** (`GET /notes/update/{id}`), la catégorie actuelle de la note
  **pré-sélectionnée**.
- La liste n'apparaît que si `getAllCategoriesActives()` renvoie **≥ 2** catégories.
- Le transfert s'applique à l'enregistrement de la modification (choix d'une autre catégorie
  dans le select).

**Hors périmètre (YAGNI)**
- La **création** de note (`/notes/add/{categorieId}`) : la catégorie reste figée par l'onglet
  cliqué (champ caché), inchangée.
- Pas de couleur de catégorie, pas de CRUD catégorie (déjà hors périmètre).
- Pas de gestion d'une note dont la catégorie actuelle serait inactive : voir §6 (hypothèse).

## 3. Comportement attendu

- Liste = **toutes les catégories actives**, triées comme `getAllCategoriesActives()` (par id),
  avec la catégorie actuelle de la note **pré-sélectionnée**. Par défaut, ouvrir le formulaire
  et enregistrer **ne change pas** la catégorie.
- Choisir une autre catégorie puis enregistrer **transfère** la note : elle disparaît de l'onglet
  d'origine et apparaît sous la catégorie de destination.
- Avec une seule catégorie active, ou en création, le `categorieId` reste porté par un **champ
  caché** (comportement actuel préservé).

## 4. Modèle de données

Aucune migration. Le transfert ne change que la valeur de `Note.categorieId` (déjà existant).
`NoteDto.categorieId` (Integer) reste le porteur du choix ; il n'a **pas** de contrainte
`@NotNull` (déjà le cas).

## 5. Composants

### 5.1 Contrôleur `NotePageController`

- **Réintroduire** la dépendance `CategorieService` (injection par constructeur via
  `@RequiredArgsConstructor`), retirée au chantier précédent.
- **`GET /notes/update/{id}`** : après avoir construit le `NoteDto` via
  `new NoteDto().fromModel(note)`, charger `categorieService.getAllCategoriesActives()` ; **si
  la liste contient ≥ 2 catégories**, l'ajouter au modèle sous l'attribut `categories`. Sinon,
  ne rien ajouter (le template retombe sur le champ caché).
- **`POST /notes/update/{id}`** : sur la branche d'erreur de validation
  (`resultat.hasErrors()`), **re-peupler** l'attribut `categories` selon la même condition (≥ 2
  actives) avant de retourner la vue `edition`, afin que le select survive à une erreur de
  saisie. Le chemin nominal est **inchangé** : `noteService.updateNote(id, note)` applique déjà
  `note.categorieId` (le transfert fonctionne sans logique supplémentaire).
- **`add` (GET et POST)** : inchangé — aucun attribut `categories`, donc champ caché.

Pour éviter la duplication, la sélection conditionnelle (« liste active ≥ 2 → `categories` »)
peut être factorisée dans une petite méthode privée (commentaire `//`, pas de Javadoc).

### 5.2 Vue `edition.html`

Remplacer le champ caché unique `<input type="hidden" th:field="*{categorieId}"/>` par un bloc
conditionnel :

- **Si `categories` est présent** : un groupe de formulaire avec un libellé « Catégorie » et un
  `<select class="form-select" th:field="*{categorieId}">` dont les options itèrent sur
  `${categories}` (`th:value="${c.id}"`, `th:text="${c.denomination}"`). La pré-sélection de la
  catégorie courante est automatique car `th:field` lie `note.categorieId`.
- **Sinon** : `<input type="hidden" th:field="*{categorieId}"/>` (création, ou < 2 actives).

Position : en haut du formulaire (avant le titre) ou juste après, au choix de l'implémentation,
de façon cohérente avec la mise en page Bootstrap existante.

## 6. Hypothèse

Une note atteignable depuis l'interface est toujours dans une **catégorie active** : le tableau
(`ListeNotesService.getTableau()`) ne liste que les catégories actives, donc on n'ouvre jamais le
formulaire d'une note rattachée à une catégorie inactive. La catégorie courante figure donc
toujours dans la liste des actives et reste correctement pré-sélectionnée. Ce cas limite n'est
pas traité spécifiquement.

## 7. Sécurité

Aucune modification de `SecurityConfiguration`. La route `/notes/**` est déjà `authenticated()`,
le POST passe par le CSRF déjà actif (token injecté par le formulaire Thymeleaf). Le `<select>`
ne propose que des catégories actives existantes.

## 8. Tests (couverture ≥ 90 %, conventions maison)

- **`@WebMvcTest` `NotePageControllerTest`** :
  - `GET /notes/update/{id}` avec **≥ 2** catégories actives (mock) → modèle contient
    `categories`, vue `edition`.
  - `GET /notes/update/{id}` avec **1** catégorie active → **pas** d'attribut `categories`.
  - `POST /notes/update/{id}` avec un `categorieId` **différent** → `updateNote` reçoit un
    `NoteDto` portant le nouveau `categorieId` (ArgumentCaptor) = transfert.
  - `POST /notes/update/{id}` en **erreur de validation** (titre vide) avec ≥ 2 actives → vue
    `edition` et attribut `categories` **re-peuplé**.
  - `GET /notes/add/{id}` → **pas** d'attribut `categories` (inchangé).
  - Réintroduire le `@MockitoBean CategorieService` dans ce test (le contrôleur en dépend de
    nouveau) et stubber `getAllCategoriesActives()` selon le scénario.
- `./mvnw verify` doit rester **vert** (Checkstyle 0 + JaCoCo ≥ 90 %).
- Le rendu visuel du select sera confirmé dans l'application réelle.

## 9. Impacts sur le code existant

- `NotePageController` : ré-ajout de l'injection `CategorieService` ; `GET /notes/update/{id}`
  et la branche d'erreur de `POST /notes/update/{id}` peuplent `categories` sous condition.
- `edition.html` : champ caché remplacé par le bloc conditionnel select / hidden.
- `NotePageControllerTest` : ré-ajout du mock `CategorieService` ; nouveaux cas de test.
- Aucun changement de `NoteService`, `CategorieService`, `NoteDto`, ni de schéma.

## 10. Livraison

Branche dédiée (`feat/transfert-categorie-note`), commits français explicites et fréquents,
build vert avant chaque commit, finalisation par merge ou PR selon le choix de l'utilisateur.
