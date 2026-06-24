// Copie du contenu d'une note dans le presse-papiers au clic (mode lecture).
// Le gestionnaire est délégué pour éviter tout script inline (compatibilité CSP).
document.addEventListener("click", function (event) {
    const cible = event.target.closest("[data-contenu]");
    if (cible) {
        navigator.clipboard.writeText(cible.dataset.contenu);
    }
});
