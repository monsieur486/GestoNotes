# Catégorie principale non désactivable — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Empêcher la désactivation d'une catégorie non modifiable (la principale) — garde service, redirection sans crash au contrôleur, et bouton de bascule masqué en vue.

**Architecture:** Une seule règle métier portée par `Categorie.estModifiable`. `CategorieService.toggleActive` refuse de désactiver une catégorie non modifiable active (lève `IllegalArgumentException`) ; `CategoriePageController.toggleActive` capture l'exception et redirige ; `categories.html` n'affiche le bouton de bascule que pour les catégories modifiables. Double garde (vue + service).

**Tech Stack:** Java 17, Spring Boot 4.0.3, Spring MVC, Thymeleaf, Lombok, JUnit 5 + Mockito + `@WebMvcTest`.

## Global Constraints

- Java 17, Maven via `./mvnw`. Construire avec `./mvnw -o verify` (hors-ligne ; retirer `-o` si un artefact manque).
- Conventions maison : Javadoc française (description + `<p><b>Exemple :</b> …</p>` AVANT les balises `@`) sur le public ; commentaire `//` sur les privées ; logs SLF4J (info flux nominal, warn anomalie récupérable), messages français paramétrés `{}` ; imports explicites (pas de wildcard) ; largeur ≤ 120 ; aucun code mort.
- Checkstyle (validate, bloquant) + JaCoCo **≥ 90 %** doivent passer à chaque `verify`.
- Commits en **français**, Conventional Commits ; contributeur unique `monsieur486`, **aucun** trailer `Co-Authored-By:`.
- Branche : `feat/principale-non-desactivable` (déjà créée).
- Règle : une catégorie **non modifiable** (`estModifiable = false`) **et active** ne peut pas être désactivée. Les modifiables restent librement activables/désactivables.

---

## File Structure

| Fichier | Responsabilité | Action |
|---------|----------------|--------|
| `service/CategorieService.java` | Garde anti-désactivation dans `toggleActive` | Modifier |
| `controller/CategoriePageController.java` | `try/catch` autour de `toggleActive` + redirection | Modifier |
| `templates/categories.html` | Bouton de bascule conditionné à `estModifiable` | Modifier |
| `service/CategorieServiceTest.java` | Cas refusé + ajustement du cas nominal | Modifier |
| `controller/CategoriePageControllerTest.java` | Refus sans crash + rendu conditionnel du bouton | Modifier |

---

### Task 1: Protéger la catégorie principale contre la désactivation

**Files:**
- Modify: `src/main/java/com/mr486/gestonote/service/CategorieService.java`
- Modify: `src/main/java/com/mr486/gestonote/controller/CategoriePageController.java`
- Modify: `src/main/resources/templates/categories.html`
- Test: `src/test/java/com/mr486/gestonote/service/CategorieServiceTest.java`
- Test: `src/test/java/com/mr486/gestonote/controller/CategoriePageControllerTest.java`

**Interfaces:**
- Consumes: `CategorieService.getCategorieById(Integer)`, `CategorieRepository.findById/save`, `Categorie.getEstModifiable()/getEstActive()/setEstActive(Boolean)`.
- Produces: `CategorieService.toggleActive(Integer)` lève `IllegalArgumentException` si la catégorie est non modifiable **et** active (sans `save`) ; sinon bascule `estActive` comme avant. `CategoriePageController.toggleActive` redirige toujours vers `/categories`, même en cas d'exception.

- [ ] **Step 1: Mettre à jour les tests du service (rouge)**

Dans `CategorieServiceTest.java`, **remplacer** la méthode `toggleActiveBasculeLEtatActif()` par une version utilisant une catégorie **modifiable** (sinon la nouvelle garde s'appliquerait), et **ajouter** le cas de refus :

```java
    @Test
    void toggleActiveBasculeLEtatActifDUneCategorieModifiable() {
        Categorie categorie = Categorie.builder().id(2).estActive(true).estModifiable(true).build();
        when(categorieRepository.findById(2)).thenReturn(Optional.of(categorie));

        categorieService.toggleActive(2);

        verify(categorieRepository).save(categorie);
        assertThat(categorie.getEstActive()).isFalse();
    }

    @Test
    void toggleActiveRefuseDeDesactiverUneCategorieNonModifiable() {
        Categorie categorie = Categorie.builder().id(1).estActive(true).estModifiable(false).build();
        when(categorieRepository.findById(1)).thenReturn(Optional.of(categorie));

        assertThatThrownBy(() -> categorieService.toggleActive(1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(categorie.getEstActive()).isTrue();
        verify(categorieRepository, never()).save(categorie);
    }
```

(Les imports `assertThatThrownBy` et `never` sont déjà présents dans ce fichier.)

- [ ] **Step 2: Mettre à jour les tests du contrôleur (rouge)**

Dans `CategoriePageControllerTest.java`, **ajouter** le cas « refus sans crash » et le cas « bouton affiché pour une catégorie modifiable », et **étendre** `afficheToutesLesCategories` (qui renvoie une catégorie **non modifiable**) pour vérifier l'absence du bouton de bascule.

Ajouter l'import statique du matcher de contenu :

```java
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
```

Étendre `afficheToutesLesCategories` en ajoutant, après les `andExpect` existants :

```java
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Désactiver"))));
```

Ajouter les deux nouveaux tests :

```java
    @Test
    @WithMockUser
    void basculeRefuseeNePlantePasEtRedirige() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Catégorie non modifiable : désactivation interdite (1)"))
                .when(categorieService).toggleActive(1);

        mockMvc.perform(post("/categories/1/toggle-active").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));
    }

    @Test
    @WithMockUser
    void afficheLeBoutonDeBasculePourUneCategorieModifiable() throws Exception {
        when(categorieService.getAllCategories()).thenReturn(java.util.List.of(
                Categorie.builder().id(2).denomination("Idées").estActive(true).estModifiable(true).build()));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Désactiver")));
    }
```

- [ ] **Step 3: Lancer les tests pour vérifier l'échec**

Run: `./mvnw -o test -Dtest=CategorieServiceTest,CategoriePageControllerTest`
Expected: échec — la garde n'existe pas (`toggleActiveRefuse…` ne lève pas), et le bouton de bascule s'affiche encore pour la catégorie non modifiable (`afficheToutesLesCategories` voit « Désactiver »).

- [ ] **Step 4: Ajouter la garde au service**

Dans `CategorieService.java`, remplacer la méthode `toggleActive` par :

```java
    /**
     * Bascule l'état actif d'une catégorie (visible ou masquée dans le tableau de notes).
     *
     * <p><b>Exemple :</b> sur une catégorie modifiable active, {@code toggleActive(2)} la
     * désactive ; sur une catégorie non modifiable active (la principale), l'appel est refusé
     * et lève {@link IllegalArgumentException}.</p>
     *
     * @param id identifiant de la catégorie
     * @throws IllegalArgumentException si la catégorie est introuvable, ou non modifiable et active
     */
    public void toggleActive(Integer id) {
        Categorie categorie = getCategorieById(id);
        if (!categorie.getEstModifiable() && categorie.getEstActive()) {
            log.warn("désactivation refusée pour la catégorie non modifiable {}", id);
            throw new IllegalArgumentException("Catégorie non modifiable : désactivation interdite (" + id + ")");
        }
        categorie.setEstActive(!categorie.getEstActive());
        categorieRepository.save(categorie);
        log.info("état actif de la catégorie {} basculé à {}", id, categorie.getEstActive());
    }
```

- [ ] **Step 5: Capturer l'exception au contrôleur**

Dans `CategoriePageController.java`, remplacer la méthode `toggleActive` par :

```java
    /**
     * Bascule l'état actif d'une catégorie puis redirige vers la page de gestion.
     *
     * <p><b>Exemple :</b> un POST sur {@code /categories/2/toggle-active} active ou désactive
     * la catégorie 2 ; sur la catégorie principale (non modifiable), l'opération est ignorée
     * sans erreur.</p>
     *
     * @param id identifiant de la catégorie
     * @return la redirection vers la page des catégories
     */
    @PostMapping("/categories/{id}/toggle-active")
    public String toggleActive(@PathVariable Integer id) {
        try {
            categorieService.toggleActive(id);
            log.info("bascule de l'état actif de la catégorie {} depuis l'interface", id);
        } catch (IllegalArgumentException ex) {
            log.warn("bascule refusée pour la catégorie {} : {}", id, ex.getMessage());
        }
        return "redirect:/categories";
    }
```

- [ ] **Step 6: Masquer le bouton en vue**

Dans `categories.html`, dans la cellule `<td class="text-end">`, ajouter `th:if="${categorie.estModifiable}"` sur le `<form>` de bascule :

```html
            <td class="text-end">
                <form th:if="${categorie.estModifiable}"
                      th:action="@{/categories/{id}/toggle-active(id=${categorie.id})}" method="post"
                      style="display:inline;">
                    <button type="submit" class="btn btn-sm btn-outline-warning"
                            th:text="${categorie.estActive} ? 'Désactiver' : 'Activer'"></button>
                </form>
            </td>
```

- [ ] **Step 7: Lancer les tests ciblés pour vérifier le succès**

Run: `./mvnw -o test -Dtest=CategorieServiceTest,CategoriePageControllerTest`
Expected: PASS (refus levé, `save` non appelé ; bouton absent pour la non modifiable, présent pour la modifiable ; refus redirige sans crash).

- [ ] **Step 8: Lancer `verify` complet**

Run: `./mvnw -o verify`
Expected: BUILD SUCCESS — Checkstyle 0, « All coverage checks have been met ».

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/mr486/gestonote/service/CategorieService.java src/main/java/com/mr486/gestonote/controller/CategoriePageController.java src/main/resources/templates/categories.html src/test/java/com/mr486/gestonote/service/CategorieServiceTest.java src/test/java/com/mr486/gestonote/controller/CategoriePageControllerTest.java
git commit -m "feat(categories): empêche la désactivation de la catégorie principale (non modifiable)"
```

---

## Auto-revue (writing-plans)

- **Couverture du spec :** §2 règle (non modifiable + active → refus) → garde Step 4. §3 comportement (bouton masqué pour non modifiable, refus serveur sans erreur) → Step 5 (contrôleur) + Step 6 (vue). §4 aucun nouveau champ → respecté (s'appuie sur `estModifiable`). §5.1 service / §5.2 contrôleur / §5.3 vue → Steps 4/5/6. §6 sécurité inchangée. §7 tests (refus + save non appelé ; nominal modifiable ; refus redirige ; bouton conditionnel) → Steps 1/2. §8 ajustement du cas nominal `toggleActive` (catégorie modifiable) → Step 1.
- **Placeholders :** aucun — chaque étape porte le code réel.
- **Cohérence des types :** `toggleActive(Integer)` lève `IllegalArgumentException` ; garde `!getEstModifiable() && getEstActive()` ; vue `th:if="${categorie.estModifiable}"` ; tests construisent les catégories avec `estModifiable` explicitement défini (évite un NPE d'unboxing) — cohérent entre code et tests.
