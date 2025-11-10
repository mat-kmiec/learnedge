package pl.learnedge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean 
    public SimpleUrlAuthenticationSuccessHandler successHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(jakarta.servlet.http.HttpServletRequest request,
                                             jakarta.servlet.http.HttpServletResponse response,
                                             org.springframework.security.core.Authentication authentication) throws java.io.IOException,
                                             jakarta.servlet.ServletException {
                log.info("Authentication success. User: {}", authentication.getName());
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ✅ CSRF włączony, ale pomijamy H2-console i API endpoints
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/api/**")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )

            // ✅ Uprawnienia
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/logowanie", "/rejestracja", "/error",
                        "/przypomnij-haslo", "/reset-hasla", "/reset-hasla/**",
                        "/oauth2/**", "/h2-console/**",
                        "/css/**", "/js/**", "/img/**", "/webjars/**",
                        "/uploads/**",  // Public profile pictures like YouTube/Instagram
                        "/api/cookies/**"  // Cookie consent API endpoints
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/rejestracja", "/przypomnij-haslo", "/reset-hasla").permitAll()
                .anyRequest().authenticated()
            )            // ✅ Dla H2
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

            // ✅ Logowanie formularzem
            .formLogin(form -> form
                .loginPage("/logowanie")
                .loginProcessingUrl("/login")
                .successHandler(successHandler())
                .defaultSuccessUrl("/panel", true)
                .failureUrl("/logowanie?error=true")
                .permitAll()
            )

            // ✅ Logowanie przez OAuth2 (Google/GitHub)
            .oauth2Login(oauth -> oauth
                .loginPage("/logowanie")
                .successHandler(successHandler())
                .defaultSuccessUrl("/panel", true)
            )

            // ✅ Sesje użytkowników
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sessionFixation -> sessionFixation.migrateSession())
                .maximumSessions(3)
                .maxSessionsPreventsLogin(false)
            )

            // ✅ Wylogowanie
            .logout(logout -> logout
                .logoutUrl("/perform-logout")
                .logoutSuccessUrl("/logowanie?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            );

        return http.build();
    }
}
