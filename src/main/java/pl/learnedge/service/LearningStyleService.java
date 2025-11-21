package pl.learnedge.service;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import pl.learnedge.model.User;
import pl.learnedge.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningStyleService {

    private final UserRepository userRepository;
    private final HuggingFaceAiService aiService;

    private static final int POINT_PER_CLOSED_ANSWER = 1;
    private static final int MIN_OPEN_ANSWER_WEIGHT = 3; 

    public enum LearningStyle {
        VISUAL, AUDITORY, KINESTHETIC, MIXED
    }

    @Data
    @Builder
    public static class AnalysisResult {
        private String dominantStyle;
        private Map<String, Double> percentages;
        private Map<String, Integer> rawScores;
    }

    public AnalysisResult analyzeAndSaveLearningStyle(Map<String, String> surveyAnswers) {
        log.info("Rozpoczynam analizę stylu uczenia. Liczba odpowiedzi: {}", surveyAnswers.size());

        Map<LearningStyle, Integer> scores = new EnumMap<>(LearningStyle.class);
        scores.put(LearningStyle.VISUAL, 0);
        scores.put(LearningStyle.AUDITORY, 0);
        scores.put(LearningStyle.KINESTHETIC, 0);

        int closedQuestionsCount = analyzeClosedQuestions(surveyAnswers, scores);

        String userDescription = surveyAnswers.get("userDescription");
        analyzeOpenQuestion(userDescription, scores, closedQuestionsCount);

        LearningStyle dominantStyle = determineDominantStyle(scores);
        Map<String, Double> percentages = calculatePercentages(scores);

        saveResultToUser(dominantStyle.name());

        log.info("Wynik: {}, Detale: {}", dominantStyle, scores);

        return AnalysisResult.builder()
                .dominantStyle(dominantStyle.name())
                .percentages(percentages)
                .rawScores(scores.entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue)))
                .build();
    }

    private int analyzeClosedQuestions(Map<String, String> answers, Map<LearningStyle, Integer> scores) {
        int count = 0;
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            if ("userDescription".equals(entry.getKey()) || entry.getValue() == null) continue;

            LearningStyle style = mapStringToStyle(entry.getValue());
            if (style != LearningStyle.MIXED) {
                scores.merge(style, POINT_PER_CLOSED_ANSWER, Integer::sum);
                count++;
            }
        }
        return count;
    }

    private void analyzeOpenQuestion(String description, Map<LearningStyle, Integer> scores, int closedCount) {
        if (description == null || description.trim().length() < 3) return;

        LearningStyle detectedStyle = LearningStyle.MIXED;

        if (aiService.isAvailable()) {
            try {
                String aiResponse = aiService.analyzeLearningStyle(Map.of("userDescription", description));
                detectedStyle = mapAiResponseToStyle(aiResponse);
                log.info("AI zanalizowało opis jako: {}", detectedStyle);
            } catch (Exception e) {
                log.error("Błąd AI, używam fallbacku", e);
            }
        }

        if (detectedStyle == LearningStyle.MIXED) {
            detectedStyle = analyzeKeywords(description);
            log.info("Algorytm słów kluczowych (Fallback) wykrył: {}", detectedStyle);
        }

        if (detectedStyle != LearningStyle.MIXED) {
            int dynamicWeight = (int) Math.round(closedCount * 0.3);
            int weightToAdd = Math.max(dynamicWeight, MIN_OPEN_ANSWER_WEIGHT);
            scores.merge(detectedStyle, weightToAdd, Integer::sum);
            log.info("Dodano {} pkt do stylu {} z opisu.", weightToAdd, detectedStyle);
        }
    }

    private LearningStyle mapAiResponseToStyle(String aiResponse) {
        if (aiResponse == null) return LearningStyle.MIXED;
        String normalized = aiResponse.toUpperCase();
        if (normalized.contains("VISUAL")) return LearningStyle.VISUAL;
        if (normalized.contains("AUDITORY")) return LearningStyle.AUDITORY;
        if (normalized.contains("KINESTHETIC")) return LearningStyle.KINESTHETIC;
        return LearningStyle.MIXED;
    }

    private LearningStyle mapStringToStyle(String value) {
        try {
            return LearningStyle.valueOf(value.toUpperCase().trim());
        } catch (Exception e) {
            return LearningStyle.MIXED;
        }
    }

    private LearningStyle analyzeKeywords(String text) {
        String lower = text.toLowerCase();

        // Zaktualizowana lista (spójna z AI injection)
        int v = countOccurrences(lower, "obraz", "schemat", "wykres", "tabela", "diagram", "fotograf", "kolor", "ilustracja", "notatk", "wzrok", "widz", "visual");
        int a = countOccurrences(lower, "słuch", "dźwięk", "głos", "mów", "rozmaw", "dyskus", "muzyk", "podcast", "nagran", "wykład", "auditory");
        int k = countOccurrences(lower, "ruch", "dotyk", "praktyk", "robi", "czuj", "ciał", "ręka", "budow", "sport", "ćwicz", "kinesthetic");

        if (v > a && v > k) return LearningStyle.VISUAL;
        if (a > v && a > k) return LearningStyle.AUDITORY;
        if (k > v && k > a) return LearningStyle.KINESTHETIC;
        
        if (v > 0 && v == a) return LearningStyle.VISUAL; // Tie-breaker
        
        return LearningStyle.MIXED;
    }

    private int countOccurrences(String text, String... keywords) {
        int count = 0;
        for (String word : keywords) {
            if (text.contains(word)) count++;
        }
        return count;
    }

    private LearningStyle determineDominantStyle(Map<LearningStyle, Integer> scores) {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(LearningStyle.MIXED);
    }

    private Map<String, Double> calculatePercentages(Map<LearningStyle, Integer> scores) {
        double total = scores.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Double> percentages = new HashMap<>();
        scores.forEach((style, score) -> {
            double percent = total > 0 ? (score * 100.0 / total) : 0.0;
            percentages.put(style.name(), Math.round(percent * 10.0) / 10.0);
        });
        return percentages;
    }

    private void saveResultToUser(String style) {
        User currentUser = getAuthenticatedUser();
        currentUser.setLearningStyle(style);
        userRepository.save(currentUser);
        updateAuthenticationObject(currentUser);
    }
    
    public boolean isAiAnalysisAvailable() {
        return aiService.isAvailable();
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new IllegalStateException("Brak usera");
        return userRepository.findByUsername(auth.getName()).orElseThrow();
    }

    private void updateAuthenticationObject(User updatedUser) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(updatedUser, auth.getCredentials(), auth.getAuthorities())
        );
    }
}