package com.mr486.gestonote.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de la sécurité : règles d'accès, formulaire de connexion et utilisateur
 * applicatif unique chargé depuis la configuration.
 */
@Configuration
public class SecurityConfiguration {

    /** Nom de l'utilisateur applicatif (variable d'environnement). */
    @Value("${app.auth.user01.name}")
    private String appUser01;

    /** Mot de passe de l'utilisateur applicatif (variable d'environnement). */
    @Value("${app.auth.user01.password}")
    private String appPass01;

    /**
     * Définit la chaîne de filtres de sécurité : ressources publiques, zones protégées,
     * connexion par formulaire et déconnexion.
     *
     * <p><b>Exemple :</b> {@code /css/**} et {@code /login} sont publics, {@code /notes/**}
     * exige une authentification et {@code /admin/**} le rôle {@code ADMIN}.</p>
     *
     * @param http configurateur de sécurité HTTP fourni par Spring
     * @return la chaîne de filtres de sécurité construite
     * @throws Exception si la configuration de sécurité échoue
     */
    @Bean
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/css/**",
                                "/img/**",
                                "/js/**",
                                "/favicon.ico",
                                "/login"
                        ).permitAll()
                        .requestMatchers("/notes/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/notes", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Fournit l'encodeur de mots de passe (BCrypt).
     *
     * <p><b>Exemple :</b> {@code passwordEncoder().encode("secret")} produit un hachage
     * BCrypt vérifiable à la connexion.</p>
     *
     * @return l'encodeur BCrypt
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Déclare l'unique utilisateur applicatif, doté des rôles {@code USER} et {@code ADMIN}.
     *
     * <p><b>Exemple :</b> l'utilisateur configuré peut se connecter et accéder aux zones
     * {@code /notes/**} et {@code /admin/**}.</p>
     *
     * @param encoder encodeur utilisé pour hacher le mot de passe
     * @return le gestionnaire d'utilisateurs en mémoire
     */
    @Bean
    UserDetailsService users(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(appUser01).password(encoder.encode(appPass01)).roles("USER", "ADMIN").build()
        );
    }
}
