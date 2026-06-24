# Édition des notes & gestion des catégories — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rendre l'édition/création de notes fonctionnelle et ajouter une page de gestion des catégories (activer/désactiver + renommer).

**Architecture:** Spring MVC server-rendered (Thymeleaf), motif POST-Redirect-GET. Le formulaire d'édition lie un `NoteDto` (couleur = code Integer 1-4). La gestion des catégories réutilise `CategorieService` étendu. Aucune migration de base.

**Tech Stack:** Java 17, Spring Boot 4.0.3, Spring MVC, Spring Data JPA, Thymeleaf, Lombok, JUnit 5 + Mockito + `@WebMvcTest`, Bean Validation (hibernate-validator).

## Global Constraints

- Java 17, Maven via `./mvnw`. Construire avec `./mvnw -o verify` (hors-ligne ; retirer `-o` si un artefact manque).
- Conventions maison **obligatoires** sur tout code touché : Javadoc française (description + `<p><b>Exemple :</b> …</p>` avant les balises `@`) sur le public ; commentaire `//` sur les méthodes privées ; logs SLF4J (`info`/`warn` sur méthodes publiques métier, `debug` sur privées), messages français paramétrés `{}`, sans donnée sensible ; imports explicites (pas de wildcard) ; largeur de ligne ≤ 120 ; **aucun code mort**.
- Checkstyle (phase `validate`, bloquant) + JaCoCo **≥ 90 %** de lignes doivent passer à chaque `verify`.
- Commits en **français**, Conventional Commits, explicites et fréquents ; contributeur unique `monsieur486`, **aucun** trailer `Co-Authored-By:`.
- Branche de travail : `feat/edition-notes-categories` (déjà créée).
- Pas de couleur de catégorie, pas de CRUD catégorie, pas d'AJAX (hors périmètre).

---

## File Structure

| Fichier | Responsabilité | Action |
|---------|----------------|--------|
| `pom.xml` | Ajout de `spring-boot-starter-validation` | Modifier |
| `dto/NoteDto.java` | Contraintes `@NotBlank` titre/contenu | Modifier |
| `model/CouleurNote.java` | Extension de 4 à 8 couleurs (codes 1-8) | Modifier |
| `service/CategorieService.java` | `getAllCategories`, `toggleActive` ; retrait du code mort | Modifier |
| `persistance/CategorieRepository.java` | Retrait de `findAllByEstModifiableTrueOrderById` (mort) | Modifier |
| `controller/CategoriePageController.java` | GET liste + POST toggle/rename | Modifier |
| `controller/NotePageController.java` | GET/POST add+update sur `NoteDto` ; retrait `categorieService` | Modifier |
| `templates/categories.html` | Tableau de gestion des catégories | Modifier |
| `templates/edition.html` | Formulaire d'édition de note | Modifier |
| `templates/notes.html` | Lien « Ajouter » + clic édition en mode édition | Modifier |
| `service/CategorieServiceTest.java` | Tests `getAllCategories`/`toggleActive` ; retrait tests morts | Modifier |
| `controller/CategoriePageControllerTest.java` | Tests web catégories | Créer |
| `controller/NotePageControllerTest.java` | Tests web add/update (POST) | Modifier |
| `dto/NoteDtoValidationTest.java` | Test de validation Bean | Créer |
| `model/CouleurNoteTest.java` | Tests des 8 couleurs | Modifier |

---

### Task 1: Activer Bean Validation et contraindre `NoteDto`

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/java/com/mr486/gestonote/dto/NoteDto.java`
- Test: `src/test/java/com/mr486/gestonote/dto/NoteDtoValidationTest.java`

**Interfaces:**
- Produces: `NoteDto` avec `@NotBlank` sur `titre` et `contenu` (validables via `@Valid`).

- [ ] **Step 1: Écrire le test de validation qui échoue**

Créer `src/test/java/com/mr486/gestonote/dto/NoteDtoValidationTest.java` :

```java
package com.mr486.gestonote.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de validation Bean de {@link NoteDto}.
 */
class NoteDtoValidationTest {

    private boolean champEnViolation(NoteDto dto, String champ) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(dto).stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals(champ));
        }
    }

    @Test
    void titreVideEstRejete() {
        NoteDto dto = NoteDto.builder().categorieId(1).titre("  ").couleur(1).contenu("texte").build();
        assertThat(champEnViolation(dto, "titre")).isTrue();
    }

    @Test
    void contenuVideEstRejete() {
        NoteDto dto = NoteDto.builder().categorieId(1).titre("Titre").couleur(1).contenu("").build();
        assertThat(champEnViolation(dto, "contenu")).isTrue();
    }

    @Test
    void noteCompleteEstValide() {
        NoteDto dto = NoteDto.builder().categorieId(1).titre("Titre").couleur(1).contenu("texte").build();
        assertThat(champEnViolation(dto, "titre")).isFalse();
        assertThat(champEnViolation(dto, "contenu")).isFalse();
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `./mvnw -o test -Dtest=NoteDtoValidationTest`
Expected: échec de compilation/test — `jakarta.validation` absente du classpath et annotations absentes.

- [ ] **Step 3: Ajouter la dépendance de validation**

Dans `pom.xml`, ajouter après la dépendance `spring-boot-starter-webmvc` :

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
```

- [ ] **Step 4: Annoter `NoteDto`**

Dans `NoteDto.java`, ajouter les imports et les annotations sur les champs :

```java
import jakarta.validation.constraints.NotBlank;
```

```java
    /** Titre saisi de la note. */
    @NotBlank(message = "Le titre est obligatoire")
    private String titre = "";
```

```java
    /** Contenu saisi de la note. */
    @NotBlank(message = "Le contenu est obligatoire")
    private String contenu = "";
```

- [ ] **Step 5: Lancer le test pour vérifier le succès**

Run: `./mvnw -o test -Dtest=NoteDtoValidationTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/com/mr486/gestonote/dto/NoteDto.java src/test/java/com/mr486/gestonote/dto/NoteDtoValidationTest.java
git commit -m "feat(notes): active Bean Validation et rend titre/contenu obligatoires"
```

---

### Task 2: Étendre `CategorieService` et retirer le code mort

**Files:**
- Modify: `src/main/java/com/mr486/gestonote/service/CategorieService.java`
- Modify: `src/main/java/com/mr486/gestonote/persistance/CategorieRepository.java`
- Test: `src/test/java/com/mr486/gestonote/service/CategorieServiceTest.java`

**Interfaces:**
- Produces:
  - `List<Categorie> getAllCategories()` — toutes les catégories triées par id.
  - `void toggleActive(Integer id)` — bascule `estActive`.
- Consumes: `CategorieRepository.findAll(Sort)` (Spring Data), `getCategorieById(Integer)` (existant).

- [ ] **Step 1: Écrire les tests qui échouent (et retirer les tests morts)**

Dans `CategorieServiceTest.java` : **supprimer** les méthodes `getAllCategoriesModifiablesDelegueAuDepot()` et `categorieTriggerBasculeLEtatModifiable()` (elles testent du code qui sera retiré), puis ajouter :

```java
    @Test
    void getAllCategoriesDelegueAuDepotTrieParId() {
        List<Categorie> liste = List.of(new Categorie());
        when(categorieRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))).thenReturn(liste);

        assertThat(categorieService.getAllCategories()).isSameAs(liste);
    }

    @Test
    void toggleActiveBasculeLEtatActif() {
        Categorie categorie = Categorie.builder().id(2).estActive(true).build();
        when(categorieRepository.findById(2)).thenReturn(Optional.of(categorie));

        categorieService.toggleActive(2);

        verify(categorieRepository).save(categorie);
        assertThat(categorie.getEstActive()).isFalse();
    }
```

Ajouter l'import : `import org.springframework.data.domain.Sort;`

- [ ] **Step 2: Lancer les tests pour vérifier l'échec**

Run: `./mvnw -o test -Dtest=CategorieServiceTest`
Expected: échec de compilation — `getAllCategories` / `toggleActive` n'existent pas.

- [ ] **Step 3: Modifier `CategorieService`**

Retirer la méthode `getAllCategoriesModifiables()` (et sa Javadoc) ainsi que `categorieTrigger(...)` (et sa Javadoc). Ajouter l'import `Sort` et les deux méthodes :

```java
import org.springframework.data.domain.Sort;
```

```java
    /**
     * Liste toutes les catégories (actives et inactives), triées par identifiant.
     *
     * <p><b>Exemple :</b> {@code getAllCategories()} alimente la page de gestion en
     * affichant aussi bien les catégories actives que désactivées.</p>
     *
     * @return la liste de toutes les catégories, triées par identifiant
     */
    public List<Categorie> getAllCategories() {
        return categorieRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * Bascule l'état actif d'une catégorie (visible ou masquée dans le tableau de notes).
     *
     * <p><b>Exemple :</b> sur une catégorie active, {@code toggleActive(2)} la désactive ;
     * un second appel la réactive.</p>
     *
     * @param id identifiant de la catégorie
     * @throws IllegalArgumentException si aucune catégorie ne correspond à l'identifiant
     */
    public void toggleActive(Integer id) {
        Categorie categorie = getCategorieById(id);
        categorie.setEstActive(!categorie.getEstActive());
        categorieRepository.save(categorie);
        log.info("état actif de la catégorie {} basculé à {}", id, categorie.getEstActive());
    }
```

- [ ] **Step 4: Retirer la méthode de dépôt morte**

Dans `CategorieRepository.java`, supprimer `findAllByEstModifiableTrueOrderById()` (et sa Javadoc) — elle n'a plus d'appelant après le retrait de `getAllCategoriesModifiables`.

- [ ] **Step 5: Lancer les tests pour vérifier le succès**

Run: `./mvnw -o test -Dtest=CategorieServiceTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/mr486/gestonote/service/CategorieService.java src/main/java/com/mr486/gestonote/persistance/CategorieRepository.java src/test/java/com/mr486/gestonote/service/CategorieServiceTest.java
git commit -m "feat(categories): ajoute getAllCategories et toggleActive, retire le code mort"
```

---

### Task 3: Page de gestion des catégories

**Files:**
- Modify: `src/main/java/com/mr486/gestonote/controller/CategoriePageController.java`
- Modify: `src/main/resources/templates/categories.html`
- Test: `src/test/java/com/mr486/gestonote/controller/CategoriePageControllerTest.java` (créer)

**Interfaces:**
- Consumes: `CategorieService.getAllCategories()`, `CategorieService.toggleActive(Integer)`, `CategorieService.updateCategorie(Integer, CategorieDto)`.
- Produces: routes `GET /categories`, `POST /categories/{id}/toggle-active`, `POST /categories/{id}/rename`.

- [ ] **Step 1: Écrire le test web qui échoue**

Créer `src/test/java/com/mr486/gestonote/controller/CategoriePageControllerTest.java` :

```java
package com.mr486.gestonote.controller;

import com.mr486.gestonote.configuration.SecurityConfiguration;
import com.mr486.gestonote.model.Categorie;
import com.mr486.gestonote.service.CategorieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests web de {@link CategoriePageController}, sécurité incluse.
 */
@WebMvcTest(controllers = CategoriePageController.class,
        properties = {"app.auth.user01.name=test", "app.auth.user01.password=test"})
@Import(SecurityConfiguration.class)
class CategoriePageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategorieService categorieService;

    @Test
    @WithMockUser
    void afficheToutesLesCategories() throws Exception {
        when(categorieService.getAllCategories())
                .thenReturn(List.of(Categorie.builder().id(1).denomination("Principale")
                        .estActive(true).estModifiable(false).build()));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories"))
                .andExpect(model().attributeExists("categories"));
    }

    @Test
    @WithMockUser
    void basculeLEtatActif() throws Exception {
        mockMvc.perform(post("/categories/2/toggle-active").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categorieService).toggleActive(2);
    }

    @Test
    @WithMockUser
    void renommeUneCategorie() throws Exception {
        mockMvc.perform(post("/categories/2/rename").with(csrf()).param("denomination", "Idées"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categorieService).updateCategorie(eq(2), any());
    }

    @Test
    @WithMockUser
    void renommeUneCategorieNonModifiableSansPlanter() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Catégorie non modifiable : 1"))
                .when(categorieService).updateCategorie(eq(1), any());

        mockMvc.perform(post("/categories/1/rename").with(csrf()).param("denomination", "X"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `./mvnw -o test -Dtest=CategoriePageControllerTest`
Expected: échec — routes POST inexistantes (404) et modèle `categories` absent.

- [ ] **Step 3: Implémenter le contrôleur**

Remplacer le contenu de `CategoriePageController.java` :

```java
package com.mr486.gestonote.controller;

import com.mr486.gestonote.dto.CategorieDto;
import com.mr486.gestonote.service.CategorieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrôleur de gestion des catégories : affichage, activation/désactivation et renommage.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CategoriePageController {

    private final CategorieService categorieService;

    /**
     * Affiche la page de gestion listant toutes les catégories.
     *
     * <p><b>Exemple :</b> un GET sur {@code /categories} affiche les catégories actives
     * et inactives avec leurs actions.</p>
     *
     * @param model modèle de la vue
     * @return le nom de la vue à afficher
     */
    @GetMapping("/categories")
    public String pageView(Model model) {
        model.addAttribute("page_active", "categories");
        model.addAttribute("categories", categorieService.getAllCategories());
        return "categories";
    }

    /**
     * Bascule l'état actif d'une catégorie puis redirige vers la page de gestion.
     *
     * <p><b>Exemple :</b> un POST sur {@code /categories/2/toggle-active} active ou
     * désactive la catégorie 2 et redirige vers {@code /categories}.</p>
     *
     * @param id identifiant de la catégorie
     * @return la redirection vers la page des catégories
     */
    @PostMapping("/categories/{id}/toggle-active")
    public String toggleActive(@PathVariable Integer id) {
        categorieService.toggleActive(id);
        log.info("bascule de l'état actif de la catégorie {} depuis l'interface", id);
        return "redirect:/categories";
    }

    /**
     * Renomme une catégorie modifiable puis redirige vers la page de gestion.
     *
     * <p><b>Exemple :</b> un POST sur {@code /categories/2/rename} avec
     * {@code denomination=Idées} renomme la catégorie 2 ; sur une catégorie non
     * modifiable, l'opération est ignorée sans erreur.</p>
     *
     * @param id           identifiant de la catégorie
     * @param denomination nouveau libellé
     * @return la redirection vers la page des catégories
     */
    @PostMapping("/categories/{id}/rename")
    public String rename(@PathVariable Integer id, @RequestParam String denomination) {
        try {
            categorieService.updateCategorie(id, CategorieDto.builder().denomination(denomination).build());
            log.info("renommage de la catégorie {} depuis l'interface", id);
        } catch (IllegalArgumentException ex) {
            log.warn("renommage refusé pour la catégorie {} : {}", id, ex.getMessage());
        }
        return "redirect:/categories";
    }
}
```

- [ ] **Step 4: Implémenter la vue `categories.html`**

Remplacer le bloc `container-fluid` de `categories.html` par :

```html
<div class="container-fluid">
    <h3 class="mb-3">Gestion des catégories</h3>
    <table class="table table-striped align-middle">
        <thead>
        <tr>
            <th>Catégorie</th>
            <th>État</th>
            <th class="text-end">Actions</th>
        </tr>
        </thead>
        <tbody>
        <tr th:each="categorie : ${categories}">
            <td>
                <form th:if="${categorie.estModifiable}"
                      th:action="@{/categories/{id}/rename(id=${categorie.id})}"
                      class="d-flex gap-2" method="post">
                    <input type="text" name="denomination" class="form-control form-control-sm"
                           th:value="${categorie.denomination}" required/>
                    <button type="submit" class="btn btn-sm btn-primary">Renommer</button>
                </form>
                <span th:unless="${categorie.estModifiable}" th:text="${categorie.denomination}"></span>
            </td>
            <td>
                <span th:if="${categorie.estActive}" class="badge bg-success">Active</span>
                <span th:unless="${categorie.estActive}" class="badge bg-secondary">Inactive</span>
            </td>
            <td class="text-end">
                <form th:action="@{/categories/{id}/toggle-active(id=${categorie.id})}" method="post"
                      style="display:inline;">
                    <button type="submit" class="btn btn-sm btn-outline-warning"
                            th:text="${categorie.estActive} ? 'Désactiver' : 'Activer'"></button>
                </form>
            </td>
        </tr>
        </tbody>
    </table>
</div>
```

- [ ] **Step 5: Lancer le test pour vérifier le succès**

Run: `./mvnw -o test -Dtest=CategoriePageControllerTest`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/mr486/gestonote/controller/CategoriePageController.java src/main/resources/templates/categories.html src/test/java/com/mr486/gestonote/controller/CategoriePageControllerTest.java
git commit -m "feat(categories): page de gestion (activer/désactiver, renommer)"
```

---

### Task 4: Étendre `CouleurNote` à 8 couleurs

**Files:**
- Modify: `src/main/java/com/mr486/gestonote/model/CouleurNote.java`
- Test: `src/test/java/com/mr486/gestonote/model/CouleurNoteTest.java`

**Interfaces:**
- Produces: `CouleurNote` avec 8 valeurs (codes 1-8) ; `CouleurNote.classeCss(Integer)` mappe 1→`btn btn-primary` … 8→`btn btn-dark`, défaut `btn btn-primary`.

- [ ] **Step 1: Mettre à jour le test pour exiger 8 couleurs**

Dans `CouleurNoteTest.java`, remplacer les méthodes `classeCssRetourneLaCouleurAttenduePourChaqueCode()` et `exposeLeCodeEtLaClasseCss()` par :

```java
    @Test
    void classeCssRetourneLaCouleurAttenduePourChaqueCode() {
        assertThat(CouleurNote.classeCss(1)).isEqualTo("btn btn-primary");
        assertThat(CouleurNote.classeCss(2)).isEqualTo("btn btn-success");
        assertThat(CouleurNote.classeCss(3)).isEqualTo("btn btn-warning");
        assertThat(CouleurNote.classeCss(4)).isEqualTo("btn btn-danger");
        assertThat(CouleurNote.classeCss(5)).isEqualTo("btn btn-secondary");
        assertThat(CouleurNote.classeCss(6)).isEqualTo("btn btn-info");
        assertThat(CouleurNote.classeCss(7)).isEqualTo("btn btn-light");
        assertThat(CouleurNote.classeCss(8)).isEqualTo("btn btn-dark");
    }

    @Test
    void exposeLeCodeEtLaClasseCss() {
        assertThat(CouleurNote.SUCCES.getCode()).isEqualTo(2);
        assertThat(CouleurNote.SUCCES.getClasseCss()).isEqualTo("btn btn-success");
        assertThat(CouleurNote.values()).hasSize(8);
        assertThat(CouleurNote.valueOf("FONCE")).isEqualTo(CouleurNote.FONCE);
    }
```

- [ ] **Step 2: Lancer le test pour vérifier l'échec**

Run: `./mvnw -o test -Dtest=CouleurNoteTest`
Expected: échec — codes 5-8 et `hasSize(8)` ne passent pas (seules 4 valeurs existent).

- [ ] **Step 3: Étendre l'énumération**

Dans `CouleurNote.java`, remplacer la liste des valeurs par les 8 couleurs :

```java
    /** Couleur par défaut (code 1). */
    PRIMAIRE(1, "btn btn-primary"),
    /** Couleur de succès (code 2). */
    SUCCES(2, "btn btn-success"),
    /** Couleur d'avertissement (code 3). */
    AVERTISSEMENT(3, "btn btn-warning"),
    /** Couleur de danger (code 4). */
    DANGER(4, "btn btn-danger"),
    /** Couleur secondaire / grise (code 5). */
    SECONDAIRE(5, "btn btn-secondary"),
    /** Couleur d'information / cyan (code 6). */
    INFO(6, "btn btn-info"),
    /** Couleur claire (code 7). */
    CLAIR(7, "btn btn-light"),
    /** Couleur foncée (code 8). */
    FONCE(8, "btn btn-dark");
```

- [ ] **Step 4: Lancer le test pour vérifier le succès**

Run: `./mvnw -o test -Dtest=CouleurNoteTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/mr486/gestonote/model/CouleurNote.java src/test/java/com/mr486/gestonote/model/CouleurNoteTest.java
git commit -m "feat(notes): étend la palette des notes de 4 à 8 couleurs Bootstrap"
```

---

### Task 5: Édition/création de note — contrôleur, formulaire, sauvegarde

**Files:**
- Modify: `src/main/java/com/mr486/gestonote/controller/NotePageController.java`
- Modify: `src/main/resources/templates/edition.html`
- Test: `src/test/java/com/mr486/gestonote/controller/NotePageControllerTest.java`

**Interfaces:**
- Consumes: `NoteService.getNoteById(Integer)`, `NoteService.addNote(NoteDto)`, `NoteService.updateNote(Integer, NoteDto)`, `ListeNotesService.getTableau()`, `ListeNotesService.deleteNote(Long)`, `NoteDto.fromModel(Note)`.
- Produces: `GET/POST /notes/add/{categorieId}`, `GET/POST /notes/update/{id}` (le formulaire lie un `NoteDto`, attribut de modèle `note` ; l'attribut `formAction` porte l'URL de soumission).

- [ ] **Step 1: Écrire/mettre à jour les tests web qui échouent**

Dans `NotePageControllerTest.java` : retirer les `@MockitoBean CategorieService categorieService;` (le contrôleur ne l'utilisera plus) et les deux tests `afficheLeFormulaireDAjout` / `afficheLeFormulaireDeModification` existants, puis ajouter :

```java
    @Test
    @WithMockUser
    void afficheLeFormulaireDAjout() throws Exception {
        mockMvc.perform(get("/notes/add/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"))
                .andExpect(model().attributeExists("note", "formAction"));
    }

    @Test
    @WithMockUser
    void afficheLeFormulaireDeModification() throws Exception {
        when(noteService.getNoteById(5))
                .thenReturn(Note.builder().id(5).categorieId(2).titre("T").couleur(2).contenu("C").build());

        mockMvc.perform(get("/notes/update/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"))
                .andExpect(model().attributeExists("note", "formAction"));
    }

    @Test
    @WithMockUser
    void creeUneNotePuisRedirige() throws Exception {
        mockMvc.perform(post("/notes/add/2").with(csrf())
                        .param("titre", "Courses").param("couleur", "2").param("contenu", "Pain"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes?modeEdit=true"));

        verify(noteService).addNote(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void refuseLaCreationSiTitreVide() throws Exception {
        mockMvc.perform(post("/notes/add/2").with(csrf())
                        .param("titre", "").param("couleur", "2").param("contenu", "Pain"))
                .andExpect(status().isOk())
                .andExpect(view().name("edition"));

        verify(noteService, org.mockito.Mockito.never()).addNote(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser
    void metAJourUneNotePuisRedirige() throws Exception {
        mockMvc.perform(post("/notes/update/5").with(csrf())
                        .param("categorieId", "2").param("titre", "T2").param("couleur", "3").param("contenu", "C2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notes?modeEdit=true"));

        verify(noteService).updateNote(org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.any());
    }
```

Ajouter l'import statique : `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;`

- [ ] **Step 2: Lancer les tests pour vérifier l'échec**

Run: `./mvnw -o test -Dtest=NotePageControllerTest`
Expected: échec — POST add/update inexistants, attribut `formAction` absent.

- [ ] **Step 3: Réécrire `NotePageController`**

Remplacer le contenu de `NotePageController.java` (retire `CategorieService` ; les GET construisent un `NoteDto` ; ajoute les POST) :

```java
package com.mr486.gestonote.controller;

import com.mr486.gestonote.dto.NoteDto;
import com.mr486.gestonote.service.ListeNotesService;
import com.mr486.gestonote.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Contrôleur du tableau de notes : affichage, suppression, et formulaires d'édition/création.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class NotePageController {

    private final ListeNotesService listeNotesService;
    private final NoteService noteService;

    /**
     * Affiche le tableau des notes, éventuellement en mode édition.
     *
     * <p><b>Exemple :</b> un GET sur {@code /notes?modeEdit=true} affiche le tableau avec
     * les actions d'édition visibles.</p>
     *
     * @param model    modèle de la vue
     * @param modeEdit {@code true} pour activer le mode édition (optionnel)
     * @return le nom de la vue à afficher
     */
    @GetMapping(value = "/notes")
    public String pageView(Model model, @RequestParam(required = false) Boolean modeEdit) {
        model.addAttribute("page_active", "notes");
        model.addAttribute("categories", listeNotesService.getTableau());
        model.addAttribute("modeEdit", modeEdit != null && modeEdit);
        return "notes";
    }

    /**
     * Supprime une note puis redirige vers le tableau.
     *
     * <p><b>Exemple :</b> un DELETE sur {@code /notes/delete/5} supprime la note 5.</p>
     *
     * @param id identifiant de la note à supprimer
     * @return la redirection vers le tableau des notes
     */
    @DeleteMapping(value = "/notes/delete/{id}")
    public String deleteNote(@PathVariable Long id) {
        listeNotesService.deleteNote(id);
        log.info("suppression de la note {} depuis l'interface", id);
        return "redirect:/notes";
    }

    /**
     * Affiche le formulaire de création d'une note dans une catégorie.
     *
     * <p><b>Exemple :</b> un GET sur {@code /notes/add/2} ouvre un formulaire vierge
     * rattaché à la catégorie 2.</p>
     *
     * @param categorieId identifiant de la catégorie de rattachement
     * @param model       modèle de la vue
     * @return le nom de la vue d'édition
     */
    @GetMapping(value = "/notes/add/{categorieId}")
    public String addNote(@PathVariable Integer categorieId, Model model) {
        model.addAttribute("page_active", "edition");
        NoteDto note = new NoteDto();
        note.setCategorieId(categorieId);
        model.addAttribute("note", note);
        model.addAttribute("formAction", "/notes/add/" + categorieId);
        return "edition";
    }

    /**
     * Affiche le formulaire de modification d'une note existante.
     *
     * <p><b>Exemple :</b> un GET sur {@code /notes/update/5} ouvre le formulaire
     * pré-rempli avec les données de la note 5.</p>
     *
     * @param id    identifiant de la note à modifier
     * @param model modèle de la vue
     * @return le nom de la vue d'édition
     */
    @GetMapping(value = "/notes/update/{id}")
    public String updateNoteForm(@PathVariable Integer id, Model model) {
        model.addAttribute("page_active", "edition");
        model.addAttribute("note", new NoteDto().fromModel(noteService.getNoteById(id)));
        model.addAttribute("formAction", "/notes/update/" + id);
        return "edition";
    }

    /**
     * Crée une note dans une catégorie puis redirige vers le tableau en mode édition.
     *
     * <p><b>Exemple :</b> un POST sur {@code /notes/add/2} avec un titre et un contenu
     * crée la note dans la catégorie 2 ; un formulaire invalide ré-affiche l'édition.</p>
     *
     * @param categorieId identifiant de la catégorie de rattachement
     * @param note        données saisies de la note
     * @param resultat    résultat de la validation
     * @param model       modèle de la vue
     * @return la redirection vers le tableau, ou la vue d'édition si la saisie est invalide
     */
    @PostMapping(value = "/notes/add/{categorieId}")
    public String addNoteSubmit(@PathVariable Integer categorieId, @Valid @ModelAttribute("note") NoteDto note,
                                BindingResult resultat, Model model) {
        if (resultat.hasErrors()) {
            model.addAttribute("formAction", "/notes/add/" + categorieId);
            return "edition";
        }
        note.setCategorieId(categorieId);
        noteService.addNote(note);
        return "redirect:/notes?modeEdit=true";
    }

    /**
     * Met à jour une note puis redirige vers le tableau en mode édition.
     *
     * <p><b>Exemple :</b> un POST sur {@code /notes/update/5} remplace le titre, la
     * couleur et le contenu de la note 5 ; un formulaire invalide ré-affiche l'édition.</p>
     *
     * @param id       identifiant de la note à modifier
     * @param note     données saisies de la note
     * @param resultat résultat de la validation
     * @param model    modèle de la vue
     * @return la redirection vers le tableau, ou la vue d'édition si la saisie est invalide
     */
    @PostMapping(value = "/notes/update/{id}")
    public String updateNoteSubmit(@PathVariable Integer id, @Valid @ModelAttribute("note") NoteDto note,
                                   BindingResult resultat, Model model) {
        if (resultat.hasErrors()) {
            model.addAttribute("formAction", "/notes/update/" + id);
            return "edition";
        }
        noteService.updateNote(id, note);
        return "redirect:/notes?modeEdit=true";
    }
}
```

- [ ] **Step 4: Implémenter le formulaire `edition.html`**

Remplacer le bloc `container-fluid` de `edition.html` par :

```html
<div class="container-fluid">
    <h3 class="mb-3">Édition de la note</h3>
    <form th:action="@{${formAction}}" th:object="${note}" method="post" class="col-md-6">
        <input type="hidden" th:field="*{categorieId}"/>

        <div class="mb-3">
            <label for="titre" class="form-label">Titre</label>
            <input type="text" id="titre" th:field="*{titre}" class="form-control"/>
            <div class="text-danger" th:if="${#fields.hasErrors('titre')}" th:errors="*{titre}"></div>
        </div>

        <div class="mb-3">
            <label class="form-label">Couleur</label>
            <div class="d-flex flex-wrap gap-2">
                <label class="btn btn-primary">
                    <input type="radio" th:field="*{couleur}" value="1" hidden/> Bleu
                </label>
                <label class="btn btn-success">
                    <input type="radio" th:field="*{couleur}" value="2" hidden/> Vert
                </label>
                <label class="btn btn-warning">
                    <input type="radio" th:field="*{couleur}" value="3" hidden/> Jaune
                </label>
                <label class="btn btn-danger">
                    <input type="radio" th:field="*{couleur}" value="4" hidden/> Rouge
                </label>
                <label class="btn btn-secondary">
                    <input type="radio" th:field="*{couleur}" value="5" hidden/> Gris
                </label>
                <label class="btn btn-info">
                    <input type="radio" th:field="*{couleur}" value="6" hidden/> Cyan
                </label>
                <label class="btn btn-light border">
                    <input type="radio" th:field="*{couleur}" value="7" hidden/> Clair
                </label>
                <label class="btn btn-dark">
                    <input type="radio" th:field="*{couleur}" value="8" hidden/> Foncé
                </label>
            </div>
        </div>

        <div class="mb-3">
            <label for="contenu" class="form-label">Contenu</label>
            <textarea id="contenu" th:field="*{contenu}" class="form-control" rows="6"></textarea>
            <div class="text-danger" th:if="${#fields.hasErrors('contenu')}" th:errors="*{contenu}"></div>
        </div>

        <button type="submit" class="btn btn-primary">💾 Enregistrer</button>
        <a th:href="@{/notes(modeEdit=true)}" class="btn btn-secondary">Annuler</a>
    </form>
</div>
```

- [ ] **Step 5: Lancer les tests pour vérifier le succès**

Run: `./mvnw -o test -Dtest=NotePageControllerTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/mr486/gestonote/controller/NotePageController.java src/main/resources/templates/edition.html src/test/java/com/mr486/gestonote/controller/NotePageControllerTest.java
git commit -m "feat(notes): formulaire d'édition/création de note avec sauvegarde et validation"
```

---

### Task 6: Câbler le tableau de notes (ajout + clic édition)

**Files:**
- Modify: `src/main/resources/templates/notes.html`

**Interfaces:**
- Consumes: routes `GET /notes/add/{id}` et `GET /notes/update/{id}` (Task 5).

- [ ] **Step 1: Corriger le lien « Ajouter une note » et le clic en mode édition**

Dans `notes.html`, remplacer le lien d'ajout :

```html
                <a th:if="${modeEdit}"
                   th:href="@{/notes/add/{id}(id=${categorie.id})}"
                   class="btn btn-success btn-sm">
                    ➕ Ajouter une note
                </a>
```

Puis remplacer le bloc `<span th:each="note ...">` pour que, **en mode édition**, le bouton de note soit un lien vers le formulaire de modification, et **en mode normal** conserve la copie presse-papier :

```html
            <span th:each="note : ${categorie.notes}" class="d-inline-flex align-items-center m-2">
                <a th:if="${modeEdit}"
                   th:href="@{/notes/update/{id}(id=${note.id})}"
                   th:class="${note.couleur} + ' rounded-3 p-3 text-decoration-none'"
                   th:text="${note.titre}"></a>
                <button th:unless="${modeEdit}"
                        onclick="navigator.clipboard.writeText(this.dataset.contenu)"
                        th:class="${note.couleur} + ' rounded-3 p-3'"
                        th:data-contenu="${note.contenu}"
                        th:text="${note.titre}"
                        type="button">
                </button>
                <form th:if="${modeEdit}"
                      th:action="@{/notes/delete/{id}(id=${note.id})}"
                      method="post"
                      style="display:inline;">
                    <input type="hidden" name="_method" value="delete"/>
                    <button type="submit" class="btn btn-danger btn-sm ms-1">🗑️</button>
                </form>
            </span>
```

- [ ] **Step 2: Vérifier le rendu et le flux dans l'application réelle**

Lancer la base puis l'application :

```bash
cp dist.env .env   # si .env absent
./dev-start.sh
./mvnw -o spring-boot:run
```

Se connecter (identifiants du `.env`), aller sur `/notes`, passer en **Mode Edition** et vérifier :
- « ➕ Ajouter une note » ouvre le formulaire et l'enregistrement crée la note ;
- cliquer une note ouvre le formulaire pré-rempli ; l'enregistrement met à jour titre/couleur/contenu ;
- en **Mode Normal**, cliquer une note copie bien son contenu dans le presse-papier ;
- la page `/categories` permet d'activer/désactiver et de renommer (sauf la principale).

Arrêter avec `Ctrl-C` puis `./dev-stop.sh`.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/notes.html
git commit -m "feat(notes): ouvre le formulaire d'édition au clic en mode édition et répare l'ajout"
```

---

### Task 7: Vérification finale (conventions)

**Files:** aucun (vérification et finalisation).

- [ ] **Step 1: Build complet + couverture**

Run: `./mvnw -o verify`
Expected: BUILD SUCCESS — Checkstyle 0 violation, « All coverage checks have been met » (JaCoCo ≥ 90 %), tous les tests verts.

- [ ] **Step 2: Site Maven**

Run: `./mvnw -o clean verify site`
Expected: BUILD SUCCESS — `target/site/` régénéré (informations, Javadoc FR, JaCoCo, Surefire).

- [ ] **Step 3: Contrôle sécurité rapide**

Lancer la skill `java-quality:java-security-check` sur les nouveaux contrôleurs (POST/CSRF). Attendu : aucune régression (CSRF actif, routes authentifiées, pas de secret).

- [ ] **Step 4: Finaliser la branche**

Via la skill `superpowers:finishing-a-development-branch` : vérifier les tests, puis merge dans `master` ou PR selon le choix de l'utilisateur.

---

## Auto-revue (writing-plans)

- **Couverture du spec :** §3 entités inchangées (aucune migration) + `CouleurNote` étendue à 8 → Task 4 ; §4 édition/création note → Task 1 (validation), Task 5 (contrôleur+form, 8 radios), Task 6 (notes.html) ; §5 gestion catégories → Task 2 (service), Task 3 (contrôleur+vue) ; §6 sécurité → inchangée (vérifiée Task 7) ; §7 tests → présents dans chaque tâche + Task 7 ; §8 impacts (validation, `CouleurNote` 8 couleurs, retrait `categorieService` de NotePageController, retrait code mort) → couverts.
- **Placeholders :** aucun — chaque étape porte le code réel.
- **Cohérence des types :** `getAllCategories()`, `toggleActive(Integer)`, `addNote(NoteDto)`, `updateNote(Integer, NoteDto)`, `NoteDto.fromModel(Note)`, `CouleurNote.classeCss(Integer)` (8 codes), attribut modèle `note` (NoteDto) et `formAction` — noms identiques entre tâches.
