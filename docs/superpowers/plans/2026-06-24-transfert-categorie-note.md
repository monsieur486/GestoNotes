# Transfert de catégorie d'une note — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permettre, en modification d'une note, de la transférer vers une autre catégorie via une liste déroulante (affichée seulement si ≥ 2 catégories actives).

**Architecture:** Spring MVC server-rendered. Le `GET /notes/update/{id}` (et la branche d'erreur du POST) ajoute au modèle les catégories actives quand il y en a ≥ 2 ; `edition.html` rend alors un `<select>` lié à `note.categorieId` (au lieu d'un champ caché). La sauvegarde update applique déjà `note.categorieId`, donc le transfert ne demande aucune logique de save supplémentaire.

**Tech Stack:** Java 17, Spring Boot 4.0.3, Spring MVC, Thymeleaf + Bootstrap 5.3.8, Lombok, JUnit 5 + Mockito + `@WebMvcTest`.

## Global Constraints

- Java 17, Maven via `./mvnw`. Construire avec `./mvnw -o verify` (hors-ligne ; retirer `-o` si un artefact manque).
- Conventions maison sur tout code touché : Javadoc française (description + `<p><b>Exemple :</b> …</p>` AVANT les balises `@`) sur le public ; commentaire `//` sur les méthodes privées ; logs SLF4J (info sur méthodes publiques métier), messages français paramétrés `{}` ; imports explicites (pas de wildcard) ; largeur ≤ 120 ; aucun code mort.
- Checkstyle (validate, bloquant) + JaCoCo **≥ 90 %** de lignes doivent passer à chaque `verify`.
- Commits en **français**, Conventional Commits ; contributeur unique `monsieur486`, **aucun** trailer `Co-Authored-By:`.
- Branche : `feat/transfert-categorie-note` (déjà créée).
- Périmètre : modification uniquement. Création (`/notes/add/...`) inchangée. Liste = toutes les catégories actives, courante pré-sélectionnée, affichée si `getAllCategoriesActives().size() >= 2`.

---

## File Structure

| Fichier | Responsabilité | Action |
|---------|----------------|--------|
| `controller/NotePageController.java` | Réinjecte `CategorieService` ; peuple `categories` (GET update + POST update erreur) si ≥ 2 actives | Modifier |
| `templates/edition.html` | Bloc conditionnel `<select>` (transfert) / `<input hidden>` | Modifier |
| `controller/NotePageControllerTest.java` | Mock `CategorieService` ; cas de test transfert / conditions | Modifier |

---

### Task 1: Contrôleur — peupler les catégories actives en modification

**Files:**
- Modify: `src/main/java/com/mr486/gestonote/controller/NotePageController.java`
- Test: `src/test/java/com/mr486/gestonote/controller/NotePageControllerTest.java`

**Interfaces:**
- Consumes: `CategorieService.getAllCategoriesActives()` → `List<Categorie>` ; `NoteService.getNoteById(Integer)`, `updateNote(Integer, NoteDto)` ; `NoteDto.fromModel(Note)`.
- Produces: en `GET /notes/update/{id}` et sur la branche d'erreur de `POST /notes/update/{id}`, l'attribut de modèle `categories` (List<Categorie>) est présent **si et seulement si** `getAllCategoriesActives().size() >= 2`. `add` n'expose jamais `categories`.

- [ ] **Step 1: Écrire les tests qui échouent**

Dans `NotePageControllerTest.java` : ajouter le mock du service et les nouveaux cas. D'abord, ajouter le champ mock (après les `@MockitoBean` existants) :

```java
    @MockitoBean
    private com.mr486.gestonote.service.CategorieService categorieService;
```

Remplacer le test `afficheLeFormulaireDeModification` par les deux cas conditionnels, et ajouter le test de transfert et le cas d'erreur :

```java
    @Test
    @WithMockUser
    void afficheLeFormulaireDeModificationAvecTransfertSiAuMoinsDeuxActives() throws Exception {
        when(noteService.getNoteById(5))
                .thenReturn(com.mr486.gestonote.model.Note.builder()
                        .id(5).categorieId(2).titre("T").couleur(2).contenu("C").build());
        when(categorieService.getAllCategoriesActives()).thenReturn(java.util.List.of(
                com.mr486.gestonote.model.Categorie.builder().id(1).denomination("Principale").build(),
                com.mr486.gestonote.model.Categorie.builder().id(2).denomination("Idées").build()));

        mockMvc.perform(get("/notes/update/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"))
                .andExpect(model().attributeExists("note", "formAction", "categories"));
    }

    @Test
    @WithMockUser
    void afficheLeFormulaireDeModificationSansTransfertSiUneSeuleActive() throws Exception {
        when(noteService.getNoteById(5))
                .thenReturn(com.mr486.gestonote.model.Note.builder()
                        .id(5).categorieId(1).titre("T").couleur(2).contenu("C").build());
        when(categorieService.getAllCategoriesActives()).thenReturn(java.util.List.of(
                com.mr486.gestonote.model.Categorie.builder().id(1).denomination("Principale").build()));

        mockMvc.perform(get("/notes/update/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"))
                .andExpect(model().attributeDoesNotExist("categories"));
    }

    @Test
    @WithMockUser
    void transfereLaNoteVersUneAutreCategorie() throws Exception {
        mockMvc.perform(post("/notes/update/5").with(csrf())
                        .param("categorieId", "3").param("titre", "T").param("couleur", "2").param("contenu", "C"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes?modeEdit=true"));

        org.mockito.ArgumentCaptor<NoteDto> captor = org.mockito.ArgumentCaptor.forClass(NoteDto.class);
        verify(noteService).updateNote(org.mockito.ArgumentMatchers.eq(5), captor.capture());
        assertThat(captor.getValue().getCategorieId()).isEqualTo(3);
    }

    @Test
    @WithMockUser
    void refuseLaModificationSiTitreVideEtRepeupleLesCategories() throws Exception {
        when(categorieService.getAllCategoriesActives()).thenReturn(java.util.List.of(
                com.mr486.gestonote.model.Categorie.builder().id(1).denomination("Principale").build(),
                com.mr486.gestonote.model.Categorie.builder().id(2).denomination("Idées").build()));

        mockMvc.perform(post("/notes/update/5").with(csrf())
                        .param("categorieId", "2").param("titre", "").param("couleur", "2").param("contenu", "C"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"))
                .andExpect(model().attributeExists("categories"));

        verify(noteService, org.mockito.Mockito.never())
                .updateNote(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any());
    }
```

Mettre à jour `afficheLeFormulaireDAjout` pour confirmer qu'aucune catégorie n'est exposée à la création :

```java
    @Test
    @WithMockUser
    void afficheLeFormulaireDAjout() throws Exception {
        mockMvc.perform(get("/notes/add/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"))
                .andExpect(model().attributeExists("note", "formAction"))
                .andExpect(model().attributeDoesNotExist("categories"));
    }
```

S'assurer que `import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;` est présent (il l'est déjà ; les autres références utilisent des FQN inline pour éviter d'ajouter des imports).

- [ ] **Step 2: Lancer les tests pour vérifier l'échec**

Run: `./mvnw -o test -Dtest=NotePageControllerTest`
Expected: échec de compilation (le champ `categorieService` mocké n'a pas d'équivalent injecté) puis, une fois injecté, `afficheLeFormulaireDeModificationAvecTransfert…` échoue car `categories` n'est pas ajouté au modèle.

- [ ] **Step 3: Modifier le contrôleur**

Dans `NotePageController.java`, ajouter l'import du service et de l'entité, et la dépendance :

```java
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.service.CategorieService;
```

```java
import java.util.List;
```

Ajouter le champ injecté (après `noteService`) :

```java
    private final CategorieService categorieService;
```

Remplacer la méthode `updateNoteForm` par :

```java
    @GetMapping(value = "/notes/update/{id}")
    public String updateNoteForm(@PathVariable Integer id, Model model) {
        model.addAttribute("page_active", "edition");
        model.addAttribute("note", new NoteDto().fromModel(noteService.getNoteById(id)));
        model.addAttribute("formAction", "/notes/update/" + id);
        ajouteCategoriesSiTransfertPossible(model);
        return "edition";
    }
```

Dans `updateNoteSubmit`, compléter la branche d'erreur :

```java
        if (resultat.hasErrors()) {
            model.addAttribute("formAction", "/notes/update/" + id);
            ajouteCategoriesSiTransfertPossible(model);
            return "edition";
        }
```

Ajouter la méthode privée (à la fin de la classe) :

```java
    // Expose les catégories actives pour le transfert si au moins deux sont actives.
    private void ajouteCategoriesSiTransfertPossible(Model model) {
        List<Categorie> actives = categorieService.getAllCategoriesActives();
        if (actives.size() >= 2) {
            model.addAttribute("categories", actives);
        }
    }
```

Ne pas modifier `addNote`, `addNoteSubmit`, `pageView`, `deleteNote`.

- [ ] **Step 4: Lancer les tests pour vérifier le succès**

Run: `./mvnw -o test -Dtest=NotePageControllerTest`
Expected: PASS (tous les cas, dont les nouveaux).

- [ ] **Step 5: Lancer la suite complète**

Run: `./mvnw -o test`
Expected: BUILD SUCCESS, suite verte.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/mr486/gestonote/controller/NotePageController.java src/test/java/com/mr486/gestonote/controller/NotePageControllerTest.java
git commit -m "feat(notes): expose les catégories actives en modification pour le transfert"
```

---

### Task 2: Vue — liste déroulante de transfert dans edition.html

**Files:**
- Modify: `src/main/resources/templates/edition.html`
- Test: `src/test/java/com/mr486/gestonote/controller/NotePageControllerTest.java`

**Interfaces:**
- Consumes: l'attribut de modèle `categories` (List<Categorie>) exposé par Task 1 ; `note.categorieId` (lié via `th:field`).

- [ ] **Step 1: Renforcer le test de rendu (garde du `<select>`)**

Dans `NotePageControllerTest.java`, étendre `afficheLeFormulaireDeModificationAvecTransfertSiAuMoinsDeuxActives` pour vérifier que le `<select>` et les libellés des catégories apparaissent dans le HTML rendu. Ajouter, après les `andExpect` existants de ce test :

```java
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<select")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Idées")));
```

Ajouter l'import statique si absent :

```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
```

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `./mvnw -o test -Dtest=NotePageControllerTest#afficheLeFormulaireDeModificationAvecTransfertSiAuMoinsDeuxActives`
Expected: échec — `edition.html` n'a pas encore de `<select>` (le champ caché est rendu à la place).

- [ ] **Step 3: Modifier `edition.html`**

Remplacer la ligne du champ caché :

```html
        <input type="hidden" th:field="*{categorieId}"/>
```

par le bloc conditionnel :

```html
        <div class="mb-3" th:if="${categories != null}">
            <label for="categorieId" class="form-label">Catégorie</label>
            <select id="categorieId" th:field="*{categorieId}" class="form-select">
                <option th:each="cat : ${categories}"
                        th:value="${cat.id}"
                        th:text="${cat.denomination}"></option>
            </select>
        </div>
        <input type="hidden" th:if="${categories == null}" th:field="*{categorieId}"/>
```

- [ ] **Step 4: Lancer le test pour vérifier le succès**

Run: `./mvnw -o test -Dtest=NotePageControllerTest`
Expected: PASS (le `<select>` et « Idées » apparaissent dans le rendu ; les autres cas restent verts).

- [ ] **Step 5: Lancer `verify` complet**

Run: `./mvnw -o verify`
Expected: BUILD SUCCESS — Checkstyle 0, « All coverage checks have been met ».

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/templates/edition.html src/test/java/com/mr486/gestonote/controller/NotePageControllerTest.java
git commit -m "feat(notes): liste déroulante de transfert de catégorie dans le formulaire de modification"
```

---

## Auto-revue (writing-plans)

- **Couverture du spec :** §3 comportement (liste actives, courante pré-sélectionnée via `th:field`, transfert à l'enregistrement) → Task 1 (modèle) + Task 2 (select). §4 modèle inchangé → aucune migration. §5.1 contrôleur (réinjection `CategorieService`, `categories` conditionnel GET + POST erreur, helper privé) → Task 1. §5.2 vue (select / hidden conditionnels) → Task 2. §6 hypothèse (note toujours en catégorie active) → pas de code. §7 sécurité inchangée. §8 tests (≥2 → présent, 1 → absent, transfert captor, erreur re-peuple, add sans categories, rendu select) → Task 1 + Task 2.
- **Placeholders :** aucun — chaque étape porte le code réel.
- **Cohérence des types :** `getAllCategoriesActives()` → `List<Categorie>` ; attribut modèle `categories` ; `ajouteCategoriesSiTransfertPossible(Model)` ; `note.categorieId` (Integer) lié par `th:field` — noms identiques entre les deux tâches.
