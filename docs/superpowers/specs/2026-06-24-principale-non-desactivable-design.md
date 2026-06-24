# Catégorie principale non désactivable — Design

Date : 2026-06-24
Projet : GestoNote (Spring Boot 4 + Thymeleaf + PostgreSQL)

## 1. Objectif

Empêcher la **désactivation de la catégorie principale**. La principale est la catégorie
**système**, déjà caractérisée par `estModifiable = false` (non renommable). On étend cette
protection : une catégorie **non modifiable** ne peut pas être désactivée — elle reste toujours
visible dans le tableau de notes.

## 2. Règle métier

- Une catégorie **non modifiable** (`estModifiable = false`) **ne peut pas être désactivée**.
- Concrètement : tenter de basculer l'état actif d'une catégorie non modifiable **actuellement
  active** est refusé (cela reviendrait à la désactiver).
- Les catégories **modifiables** restent librement activables/désactivables (inchangé).

Identification de « la principale » : par `estModifiable = false` (pas d'id codé en dur). La
principale est aujourd'hui la seule catégorie non modifiable.

## 3. Comportement attendu

- Page de gestion `/categories` : le bouton **Activer/Désactiver** n'apparaît **que pour les
  catégories modifiables**. La principale n'a aucun bouton de bascule (cohérent avec son champ de
  renommage déjà masqué).
- Si une requête `POST /categories/{id}/toggle-active` cible malgré tout une catégorie non
  modifiable active (URL forgée, double soumission), elle est **refusée côté service** et le
  contrôleur **redirige sans erreur** vers `/categories` (défense en profondeur).

## 4. Modèle de données

Aucune migration. Aucun nouveau champ : la règle s'appuie sur `Categorie.estModifiable` existant.

## 5. Composants

### 5.1 Service `CategorieService.toggleActive(Integer id)`

Ajouter une garde au début : si la catégorie est **non modifiable** (`!getEstModifiable()`) **et
actuellement active** (`getEstActive()`), logguer un `warn` et lever
`IllegalArgumentException` (même style que `updateCategorie` pour une catégorie non modifiable),
**sans** appeler `save`. Sinon, comportement actuel (bascule `estActive`, log `info`, `save`).

La garde porte sur « non modifiable **et** active » afin de bloquer uniquement la *désactivation*
(et de ne pas empêcher une éventuelle réactivation d'une catégorie protégée inactive — cas
théorique).

### 5.2 Contrôleur `CategoriePageController.toggleActive`

Envelopper l'appel `categorieService.toggleActive(id)` dans un `try/catch
(IllegalArgumentException)` : sur exception, logguer un `warn` (« bascule refusée pour la
catégorie {} ») et rediriger vers `/categories`. Le chemin nominal reste une redirection vers
`/categories`. (Aligné sur le handler `rename` existant qui capture déjà `IllegalArgumentException`.)

### 5.3 Vue `categories.html`

Encadrer le formulaire du bouton Activer/Désactiver par `th:if="${categorie.estModifiable}"` afin
qu'il ne s'affiche **que pour les catégories modifiables**. La cellule « Actions » d'une catégorie
non modifiable reste donc vide (pas de bouton de bascule).

## 6. Sécurité

Aucune modification de `SecurityConfiguration`. La route `/categories/**` est déjà
`authenticated()`, le POST passe par le CSRF actif. La protection est à la fois côté vue (bouton
masqué) et côté service (garde) — double garde.

## 7. Tests (couverture ≥ 90 %, conventions maison)

- **`CategorieServiceTest` (Mockito)** :
  - `toggleActive` sur une catégorie **non modifiable et active** → lève
    `IllegalArgumentException`, `save` **jamais** appelé.
  - `toggleActive` sur une catégorie **modifiable active** → bascule à inactive, `save` appelé
    (cas nominal existant, conservé/ajusté).
- **`CategoriePageControllerTest` (`@WebMvcTest`)** :
  - `POST /categories/{id}/toggle-active` quand le service lève `IllegalArgumentException` → vue
    redirige vers `/categories` (statut 3xx), pas de crash.
  - Cas nominal existant (bascule réussie → redirection) conservé.
- `./mvnw verify` doit rester **vert** (Checkstyle 0 + JaCoCo ≥ 90 %).

## 8. Impacts sur le code existant

- `CategorieService.toggleActive` : ajout de la garde non modifiable + active.
- `CategoriePageController.toggleActive` : ajout du `try/catch` + log `warn` + redirection.
- `categories.html` : bouton de bascule conditionné à `estModifiable`.
- Tests `CategorieServiceTest` et `CategoriePageControllerTest` : nouveaux cas + ajustement du cas
  nominal de `toggleActive` (utiliser une catégorie **modifiable** pour le scénario de bascule
  réussie).
- Aucun changement de schéma, ni de `Note`, `NoteDto`, `NotePageController`.

## 9. Livraison

Branche dédiée (`feat/principale-non-desactivable`), commits français explicites, build vert avant
chaque commit, finalisation par merge ou PR selon le choix de l'utilisateur.
