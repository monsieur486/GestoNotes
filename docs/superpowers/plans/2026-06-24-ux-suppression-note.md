# UX après suppression d'une note — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Après suppression d'une note en mode édition, rester en mode édition et revenir sur l'onglet de la catégorie d'où la note a été supprimée.

**Architecture:** Le formulaire de suppression transmet la catégorie de la note ; `deleteNote` redirige vers `/notes?modeEdit=true&cat={categorieId}` ; `pageView` lit `cat` et l'expose comme `catActif` ; `notes.html` active l'onglet/panneau correspondant (repli sur le premier onglet si `catActif` absent).

**Tech Stack:** Java 17, Spring Boot 4.0.3, Spring MVC, Thymeleaf + Bootstrap 5.3.8, Lombok, JUnit 5 + Mockito + `@WebMvcTest`.

## Global Constraints

- Java 17, Maven via `./mvnw`. Construire avec `./mvnw -o verify` (hors-ligne ; retirer `-o` si un artefact manque).
- Conventions maison : Javadoc française (description + `<p><b>Exemple :</b> …</p>` AVANT les balises `@`) sur le public ; SLF4J (info flux nominal) ; imports explicites (pas de wildcard) ; largeur ≤ 120 ; aucun code mort.
- Checkstyle (validate, bloquant) + JaCoCo **≥ 90 %** doivent passer à chaque `verify`.
- Commits en **français**, Conventional Commits ; contributeur unique `monsieur486`, **aucun** trailer `Co-Authored-By:`.
- Branche : `feat/ux-suppression-note` (déjà créée).
- Comportement : suppression → `/notes?modeEdit=true&cat={categorieId}` ; GET `/notes` sans `cat` → premier onglet actif (inchangé).

---

## File Structure

| Fichier | Responsabilité | Action |
|---------|----------------|--------|
| `controller/NotePageController.java` | `deleteNote` (param + redirection) ; `pageView` (param `cat` → `catActif`) | Modifier |
| `templates/notes.html` | Champ caché `categorieId` (suppression) + onglet actif conditionnel | Modifier |
| `controller/NotePageControllerTest.java` | Ajustement test suppression + cas `catActif` | Modifier |

---

### Task 1: Conserver le mode édition et l'onglet après suppression

**Files:**
- Modify: `src/main/java/com/mr486/gestonote/controller/NotePageController.java`
- Modify: `src/main/resources/templates/notes.html`
- Test: `src/test/java/com/mr486/gestonote/controller/NotePageControllerTest.java`

**Interfaces:**
- Consumes: `ListeNotesService.deleteNote(Long)`, `ListeNotesService.getTableau()`.
- Produces: `deleteNote(Long id, Integer categorieId)` redirige vers `redirect:/notes?modeEdit=true` (+ `&cat={categorieId}` si non nul) ; `pageView(..., Integer cat)` expose l'attribut de modèle `catActif` (= `cat`, nul si absent).

- [ ] **Step 1: Adapter les tests (rouge)**

Dans `NotePageControllerTest.java`, **remplacer** le test `supprimeUneNotePuisRedirige` par une version qui transmet la catégorie et attend la redirection enrichie, et **ajouter** un test pour `catActif` :

```java
    @Test
    @WithMockUser
    void supprimeUneNotePuisRedirigeEnEditionSurLOnglet() throws Exception {
        mockMvc.perform(delete("/notes/delete/5").with(csrf()).param("categorieId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes?modeEdit=true&cat=2"));

        verify(listeNotesService).deleteNote(5L);
    }

    @Test
    @WithMockUser
    void exposeLOngletActifDemande() throws Exception {
        when(listeNotesService.getTableau()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/notes").param("modeEdit", "true").param("cat", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("notes"))
                .andExpect(model().attribute("catActif", 2));
    }
```

(Les autres tests, dont `afficheLeTableauDesNotes`, restent inchangés ; `catActif` y vaut `null`,
ce qui est sans effet sur le rendu.)

- [ ] **Step 2: Lancer les tests pour vérifier l'échec**

Run: `./mvnw -o test -Dtest=NotePageControllerTest`
Expected: échec — `deleteNote` redirige encore vers `/notes` (sans `modeEdit`/`cat`) et `pageView`
n'expose pas `catActif`.

- [ ] **Step 3: Modifier `deleteNote`**

Dans `NotePageController.java`, ajouter l'import s'il manque (`RequestParam` est déjà importé), et
remplacer la méthode `deleteNote` par :

```java
    /**
     * Supprime une note puis redirige vers le tableau, en restant en mode édition et sur
     * l'onglet de la catégorie d'origine.
     *
     * <p><b>Exemple :</b> un DELETE sur {@code /notes/delete/5} avec {@code categorieId=2}
     * supprime la note 5 et redirige vers {@code /notes?modeEdit=true&cat=2}.</p>
     *
     * @param id          identifiant de la note à supprimer
     * @param categorieId identifiant de la catégorie d'origine (pour réactiver son onglet)
     * @return la redirection vers le tableau des notes
     */
    @DeleteMapping(value = "/notes/delete/{id}")
    public String deleteNote(@PathVariable Long id, @RequestParam(required = false) Integer categorieId) {
        listeNotesService.deleteNote(id);
        log.info("suppression de la note {} depuis l'interface", id);
        String redirection = "redirect:/notes?modeEdit=true";
        if (categorieId != null) {
            redirection += "&cat=" + categorieId;
        }
        return redirection;
    }
```

- [ ] **Step 4: Modifier `pageView`**

Remplacer la méthode `pageView` par :

```java
    /**
     * Affiche le tableau des notes, éventuellement en mode édition et sur un onglet donné.
     *
     * <p><b>Exemple :</b> un GET sur {@code /notes?modeEdit=true&cat=2} affiche le tableau en
     * mode édition avec l'onglet de la catégorie 2 actif.</p>
     *
     * @param model    modèle de la vue
     * @param modeEdit {@code true} pour activer le mode édition (optionnel)
     * @param cat      identifiant de la catégorie dont l'onglet doit être actif (optionnel)
     * @return le nom de la vue à afficher
     */
    @GetMapping(value = "/notes")
    public String pageView(Model model, @RequestParam(required = false) Boolean modeEdit,
                           @RequestParam(required = false) Integer cat) {
        model.addAttribute("page_active", "notes");
        model.addAttribute("categories", listeNotesService.getTableau());
        model.addAttribute("modeEdit", modeEdit != null && modeEdit);
        model.addAttribute("catActif", cat);
        return "notes";
    }
```

- [ ] **Step 5: Modifier `notes.html`**

(a) Dans le formulaire de suppression, ajouter le champ caché de catégorie (après l'input
`_method`) :

```html
                    <input type="hidden" name="_method" value="delete"/>
                    <input type="hidden" name="categorieId" th:value="${note.categorieId}"/>
```

(b) Activer l'onglet correspondant à `catActif`. Remplacer la ligne de l'onglet :

```html
                    th:classappend="${iter.first} ? 'active'"
```

par :

```html
                    th:classappend="${(catActif != null and categorie.id == catActif) or (catActif == null and iter.first)} ? 'active'"
```

(c) Activer le panneau correspondant. Remplacer la ligne du panneau :

```html
             th:classappend="${iter.first} ? 'show active'"
```

par :

```html
             th:classappend="${(catActif != null and categorie.id == catActif) or (catActif == null and iter.first)} ? 'show active'"
```

- [ ] **Step 6: Lancer les tests ciblés pour vérifier le succès**

Run: `./mvnw -o test -Dtest=NotePageControllerTest`
Expected: PASS (redirection `/notes?modeEdit=true&cat=2` ; `catActif=2` exposé ; rendu sans erreur).

- [ ] **Step 7: Lancer `verify` complet**

Run: `./mvnw -o verify`
Expected: BUILD SUCCESS — Checkstyle 0, « All coverage checks have been met ».

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/mr486/gestonote/controller/NotePageController.java src/main/resources/templates/notes.html src/test/java/com/mr486/gestonote/controller/NotePageControllerTest.java
git commit -m "feat(notes): après suppression, reste en mode édition sur l'onglet d'origine"
```

---

## Auto-revue (writing-plans)

- **Couverture du spec :** §3 comportement (redirection `modeEdit=true&cat`, repli premier onglet) → Steps 3/5 ; §5.1 vue (champ caché + onglet/panneau conditionnels) → Step 5 ; §5.2 contrôleur (`deleteNote` param+redirection, `pageView` `cat`→`catActif`) → Steps 3/4 ; §6 hypothèse (catégorie active) → pas de code ; §7 sécurité inchangée ; §8 tests (redirection enrichie, `catActif`) → Step 1.
- **Placeholders :** aucun — chaque étape porte le code réel.
- **Cohérence des types :** `deleteNote(Long, Integer)` ; `pageView(Model, Boolean, Integer)` ; attribut modèle `catActif` (Integer) ; condition de vue `catActif != null and categorie.id == catActif` identique pour onglet et panneau — cohérent entre code, vue et tests.
