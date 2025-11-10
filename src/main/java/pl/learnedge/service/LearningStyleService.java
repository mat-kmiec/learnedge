package pl.learnedge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import pl.learnedge.model.User;
import pl.learnedge.repository.UserRepository;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningStyleService {

    private final UserRepository userRepository;
    private final HuggingFaceAiService aiService;

    public void saveLearningStyle(String learningStyle) {
        // Metoda została wyłączona - styl uczenia można ustawić tylko poprzez ankietę AI
        throw new IllegalArgumentException("Styl uczenia się można ustawić wyłącznie poprzez wypełnienie ankiety AI");
    }



    /**
     * Analizuje odpowiedzi z ankiety i automatycznie określa styl uczenia
     */
    public String analyzeAndSaveLearningStyle(Map<String, String> surveyAnswers) {
        try {
            String analyzedStyle;
            
            // Sprawdź czy AI jest dostępne
            if (aiService.isAvailable()) {
                // Użyj AI do analizy
                analyzedStyle = aiService.analyzeLearningStyle(surveyAnswers);
                log.info("Użyto AI do analizy stylu uczenia");
            } else {
                // Użyj algorytmu alternatywnego
                analyzedStyle = analyzeWithAlternativeAlgorithm(surveyAnswers);
                log.info("Użyto algorytm alternatywny do analizy stylu uczenia");
            }
            
            // Zapisz przeanalizowany styl
            User currentUser = getAuthenticatedUser();
            currentUser.setLearningStyle(analyzedStyle);
            userRepository.save(currentUser);
            
            // Update the authentication object in session
            updateAuthenticationObject(currentUser);
            
            log.info("Przeanalizowano styl uczenia dla użytkownika {}: {}", 
                     currentUser.getUsername(), analyzedStyle);
            
            return analyzedStyle;
        } catch (Exception e) {
            log.error("Błąd podczas analizy stylu uczenia: ", e);
            throw new RuntimeException("Nie udało się przeanalizować stylu uczenia", e);
        }
    }

    /**
     * Alternatywny algorytm analizy stylu uczenia (gdy AI niedostępne)
     */
    private String analyzeWithAlternativeAlgorithm(Map<String, String> surveyAnswers) {
        int visualCount = 0;
        int auditoryCount = 0;
        int kinestheticCount = 0;
        
        log.debug("Analyzing survey with {} answers", surveyAnswers.size());
        
        // Policz odpowiedzi dla każdego stylu (pytania od question1 do question20)
        for (Map.Entry<String, String> entry : surveyAnswers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Pomiń opis tekstowy - będzie analizowany osobno
            if ("userDescription".equals(key) || value == null) {
                continue;
            }
            
            switch (value.toLowerCase()) {
                case "visual":
                    visualCount++;
                    break;
                case "auditory":
                    auditoryCount++;
                    break;
                case "kinesthetic":
                    kinestheticCount++;
                    break;
            }
        }
        
        log.debug("Counts - Visual: {}, Auditory: {}, Kinesthetic: {}", 
                 visualCount, auditoryCount, kinestheticCount);
        
        // Sprawdź czy mamy wystarczająco odpowiedzi
        int totalAnswers = visualCount + auditoryCount + kinestheticCount;
        if (totalAnswers < 5) {
            log.warn("Too few answers provided: {}", totalAnswers);
            throw new RuntimeException("Za mało odpowiedzi do przeprowadzenia analizy. Proszę odpowiedzieć na więcej pytań.");
        }
        
        // Określ dominujący styl
        if (visualCount > auditoryCount && visualCount > kinestheticCount) {
            return "VISUAL";
        } else if (auditoryCount > visualCount && auditoryCount > kinestheticCount) {
            return "AUDITORY";
        } else if (kinestheticCount > visualCount && kinestheticCount > auditoryCount) {
            return "KINESTHETIC";
        } else {
            // W przypadku remisu, użyj opisu tekstowego użytkownika
            String userDescription = surveyAnswers.get("userDescription");
            if (userDescription != null && !userDescription.trim().isEmpty()) {
                String styleFromDescription = analyzeDescriptionForLearningStyle(userDescription);
                log.debug("Used description analysis due to tie, result: {}", styleFromDescription);
                return styleFromDescription;
            }
            
            // Jeśli brak opisu, wybierz styl z największą liczbą głosów (nawet przy remisie)
            if (visualCount >= auditoryCount && visualCount >= kinestheticCount) {
                return "VISUAL";
            } else if (auditoryCount >= kinestheticCount) {
                return "AUDITORY";
            } else {
                return "KINESTHETIC";
            }
        }
    }
    
    /**
     * Analizuje opis tekstowy użytkownika aby określić styl uczenia
     */
    private String analyzeDescriptionForLearningStyle(String description) {
        String lowerDescription = description.toLowerCase();
        
        // Słowa kluczowe dla każdego stylu
        String[] visualKeywords = {"obraz", "schemat", "diagram", "rysun", "kolor", "wizualn", "widz", "patrzę"};
        String[] auditoryKeywords = {"słuch", "muzyk", "głos", "rozmow", "czytam na głos", "słyszę", "dźwięk"};
        String[] kinestheticKeywords = {"ruch", "praktyk", "ręka", "dotyk", "sport", "ćwicz", "gestykuluj", "robię"};
        
        int visualScore = 0;
        int auditoryScore = 0;
        int kinestheticScore = 0;
        
        // Policz wystąpienia słów kluczowych
        for (String keyword : visualKeywords) {
            if (lowerDescription.contains(keyword)) {
                visualScore++;
            }
        }
        
        for (String keyword : auditoryKeywords) {
            if (lowerDescription.contains(keyword)) {
                auditoryScore++;
            }
        }
        
        for (String keyword : kinestheticKeywords) {
            if (lowerDescription.contains(keyword)) {
                kinestheticScore++;
            }
        }
        
        // Zwróć dominujący styl
        if (visualScore > auditoryScore && visualScore > kinestheticScore) {
            return "VISUAL";
        } else if (auditoryScore > visualScore && auditoryScore > kinestheticScore) {
            return "AUDITORY";
        } else if (kinestheticScore > visualScore && kinestheticScore > auditoryScore) {
            return "KINESTHETIC";
        } else {
            return "VISUAL"; // Domyślnie
        }
    }

    /**
     * Sprawdza czy AI jest dostępne do analizy
     */
    public boolean isAiAnalysisAvailable() {
        return aiService.isAvailable();
    }



    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Brak zalogowanego użytkownika");
        }
        String username;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            username = ud.getUsername();
        } else if (principal instanceof User u) {
            username = u.getUsername();
        } else {
            username = auth.getName();
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie został znaleziony: " + username));
    }

    private void updateAuthenticationObject(User updatedUser) {
        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth != null) {
            // Create new authentication with updated user
            UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                updatedUser, 
                currentAuth.getCredentials(), 
                currentAuth.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
    }
}
