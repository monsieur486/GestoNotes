package com.mr486.gestonote.journal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Consigne dans le journal central les exceptions qui remontent jusqu'au conteneur web.
 *
 * <p><b>IL OBSERVE, IL N'INTERCEPTE PAS.</b> {@link #resolveException} rend toujours
 * {@code null}, ce qui signifie pour Spring « je ne traite pas cette exception » : la chaîne se
 * poursuit vers les résolveurs suivants, et la réponse rendue reste exactement celle d'avant.</p>
 *
 * <p>C'est la raison d'être de cette classe plutôt que d'un {@code @ControllerAdvice} portant un
 * {@code @ExceptionHandler(Exception.class)} : ce dernier <b>changerait la réponse</b> rendue à
 * l'utilisateur. Instrumenter une application ne doit pas modifier ce qu'elle affiche quand elle
 * tombe — l'instrumentation observe, elle ne décide pas.</p>
 *
 * <p>La priorité la plus haute garantit d'être appelé avant tout résolveur qui, lui, traiterait
 * l'exception et l'empêcherait d'aller plus loin — y compris celui qui porte les
 * {@code @ControllerAdvice} déjà présents dans cette application.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class JournalObservateurErreurs implements HandlerExceptionResolver {

    /** Nom du composant tel qu'il apparaît dans le journal central. */
    private static final String COMPOSANT = "gestonotes";

    private final JournalClient journalClient;

    /**
     * Consigne l'exception, puis laisse la main.
     *
     * <p><b>Exemple :</b> une exception sur une page produit un {@code appli.erreur} portant le
     * chemin demandé et la trace complète, et l'utilisateur voit la même réponse qu'auparavant.</p>
     *
     * @param requete      la requête en cours
     * @param reponse      la réponse en cours
     * @param gestionnaire le contrôleur visé, éventuellement {@code null}
     * @param exception    l'exception remontée
     * @return toujours {@code null} : cette classe ne résout rien
     */
    @Override
    public ModelAndView resolveException(HttpServletRequest requete, HttpServletResponse reponse,
            Object gestionnaire, Exception exception) {
        StringWriter tampon = new StringWriter();
        exception.printStackTrace(new PrintWriter(tampon));

        journalClient.emettre(new JournalClient.EvenementSortant(
                COMPOSANT, "appli.erreur", "erreur",
                exception.getClass().getSimpleName() + " sur " + requete.getRequestURI()
                        + " : " + exception.getMessage(),
                tampon.toString(), null, null, Instant.now().toString()));

        return null;
    }
}
