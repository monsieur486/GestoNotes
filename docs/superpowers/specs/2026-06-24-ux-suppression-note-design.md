# UX après suppression d'une note — Design

Date : 2026-06-24
Projet : GestoNote (Spring Boot 4 + Thymeleaf + PostgreSQL)

## 1. Objectif

Améliorer l'expérience après la suppression d'une note **en mode édition** :

1. **Rester en mode édition** après la suppression (aujourd'hui on en sort).
2. **Revenir sur l'onglet de la catégorie** d'où la note a été supprimée (aujourd'hui on
   retombe toujours sur le premier onglet, « Principale »).

Cas d'usage : supprimer plusieurs notes d'affilée dans la même catégorie sans perdre ni le mode
édition ni l'onglet courant.

## 2. État actuel

- `NotePageController.deleteNote` redirige vers `redirect:/notes` (sans `modeEdit`), alors que
  l'ajout et la modification redirigent déjà vers `redirect:/notes?modeEdit=true`.
- `notes.html` active toujours le **premier** onglet/panneau
  (`th:classappend="${iter.first} ? 'active'"` et `... ? 'show active'`).

## 3. Comportement attendu

- Après suppression : redirection vers `/notes?modeEdit=true&cat={categorieId}` →
  on reste en **mode édition** et l'onglet de la catégorie `{categorieId}` est **actif**.
- Sur un GET `/notes` sans paramètre `cat`, comportement inchangé (premier onglet actif).

## 4. Modèle de données

Aucune migration, aucun nouveau champ.

## 5. Composants

### 5.1 Vue `notes.html`

- **Formulaire de suppression** : ajouter un champ caché portant la catégorie de la note
  (déjà disponible : `note.categorieId`) :
  `<input type="hidden" name="categorieId" th:value="${note.categorieId}"/>`.
- **Onglets et panneaux** : activer l'onglet correspondant à `catActif` (attribut de modèle
  exposé par le contrôleur), avec repli sur le premier onglet quand `catActif` est absent :
  - onglet : `th:classappend="${(catActif != null and categorie.id == catActif) or (catActif == null and iter.first)} ? 'active'"`
  - panneau : même condition → `'show active'`.

### 5.2 Contrôleur `NotePageController`

- **`deleteNote`** : accepter `@RequestParam(required = false) Integer categorieId`, supprimer la
  note, puis rediriger vers `redirect:/notes?modeEdit=true` en ajoutant `&cat={categorieId}`
  quand `categorieId` est non nul.
- **`pageView`** (GET `/notes`) : accepter `@RequestParam(required = false) Integer cat` et
  l'exposer au modèle sous l'attribut `catActif` (null si absent).

## 6. Hypothèse

La catégorie d'une note supprimée est toujours une catégorie **active** (donc un onglet affiché) :
le tableau ne liste que les catégories actives et la suppression ne supprime pas la catégorie.
`catActif` correspond donc toujours à un onglet présent. Si jamais il ne correspondait à aucun
onglet, le repli (`catActif == null`) ne s'appliquerait pas et aucun onglet ne serait actif — cas
théorique hors périmètre.

## 7. Sécurité

Aucune modification de `SecurityConfiguration`. La suppression reste un POST (méthode cachée
`_method=delete`) sous `/notes/**` (authentifié, CSRF actif). `categorieId` et `cat` sont de
simples entiers de navigation, sans effet de sécurité.

## 8. Tests (couverture ≥ 90 %, conventions maison)

- **`@WebMvcTest` `NotePageControllerTest`** :
  - `deleteNote` : POST `/notes/delete/{id}` avec `categorieId=2` → redirection vers
    `/notes?modeEdit=true&cat=2` ; `listeNotesService.deleteNote` appelé. (Le test existant
    `supprimeUneNotePuisRedirige`, qui attend `redirectedUrl("/notes")`, est ajusté.)
  - `pageView` : GET `/notes?modeEdit=true&cat=2` → modèle contient `catActif = 2` ; GET `/notes`
    sans `cat` → `catActif` absent/nul.
- `./mvnw verify` doit rester **vert** (Checkstyle 0 + JaCoCo ≥ 90 %).

## 9. Impacts sur le code existant

- `NotePageController.deleteNote` : nouveau `@RequestParam categorieId` + redirection enrichie.
- `NotePageController.pageView` : nouveau `@RequestParam cat` + attribut `catActif`.
- `notes.html` : champ caché `categorieId` dans le formulaire de suppression + activation
  conditionnelle de l'onglet/panneau.
- `NotePageControllerTest` : ajustement du test de suppression + cas `catActif`.
- Aucun changement de schéma, de service, ni des autres contrôleurs.

## 10. Livraison

Branche dédiée (`feat/ux-suppression-note`), commits français explicites, build vert avant
chaque commit, finalisation par merge ou PR selon le choix de l'utilisateur.
