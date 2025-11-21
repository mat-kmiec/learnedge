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

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LearningStyleService {

    private final UserRepository userRepository;
    private final HuggingFaceAiService aiService;

    private static final int POINT_PER_CLOSED_ANSWER = 1;
    private static final int MIN_OPEN_ANSWER_WEIGHT = 4;
    private static final int MAX_OPEN_ANSWER_WEIGHT = 8;

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
        log.info("=== START ANALIZY (MAX KEYWORDS EDITION) ===");
        
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
        log.info("=== KONIEC: Wygrał styl: {} ===", dominantStyle);

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
        if (description == null || description.trim().length() < 5) return;

        LearningStyle detectedStyle = LearningStyle.MIXED;

        // 1. Próba AI
        if (aiService.isAvailable()) {
            try {
                String aiResponse = aiService.analyzeLearningStyle(Map.of("userDescription", description));
                detectedStyle = mapAiResponseToStyle(aiResponse);
                log.info("AI Wynik: {}", detectedStyle);
            } catch (Exception e) {
                log.error("AI Błąd: {}", e.getMessage());
            }
        }

        // 2. Fallback
        if (detectedStyle == LearningStyle.MIXED) {
            log.info("Uruchamiam algorytm słów kluczowych (Fallback)...");
            detectedStyle = analyzeKeywords(description);
        }

        if (detectedStyle != LearningStyle.MIXED) {
            int dynamicWeight = (int) Math.ceil(closedCount * 0.4);
            int weightToAdd = Math.min(MAX_OPEN_ANSWER_WEIGHT, Math.max(dynamicWeight, MIN_OPEN_ANSWER_WEIGHT));
            scores.merge(detectedStyle, weightToAdd, Integer::sum);
            log.info("DODANO {} pkt do stylu {}", weightToAdd, detectedStyle);
        } else {
            log.info("BRAK PUNKTÓW z opisu.");
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

    private String normalizeText(String text) {
        if (text == null) return "";
        String nfdNormalizedString = Normalizer.normalize(text, Normalizer.Form.NFD); 
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase();
    }

    private LearningStyle analyzeKeywords(String text) {
        String normalizedText = normalizeText(text); 
        log.info("Znormalizowany tekst: '{}'", normalizedText);

        // --- WZROKOWIEC (VISUAL) ---
        // Zawiera: czytanie, pisanie, oglądanie, kolory, formy wizualne
        Set<String> visualWords = Set.of(
            "wzrok", "widz", "wizual", "visual", "oczami", "patrz", "obserw",
            "obraz", "schemat", "wykres", "tabela", "diagram", "fotograf", "zdjeci", "kolor", 
            "ilustracja", "rys", "malow", "poster", "plakat", "map", "slajd", "prezentacj", 
            "czyta", "tekst", "ksiaz", "pis", "notatk", "podkresl", "artykul", "blog", "pdf",
            "film", "video", "wideo", "ekran", "monitor", "wyobraz", "infograf"
        );
        
        // --- SŁUCHOWIEC (AUDITORY) ---
        // Zawiera: słuchanie, mówienie, dźwięki, muzykę
        Set<String> auditoryWords = Set.of(
            "sluch", "audyt", "auditory", "uszy", "uch", "dzwiek", "glos", "halas", "brzmi",
            "mow", "rozmaw", "dyskus", "gad", "opowiad", "tlumacz", "pyta", "dialog", "debat",
            "muzyk", "melodi", "rytm", "piosenk", "spiew",
            "podcast", "nagran", "audio", "radio", "mp3", "wyklad", "recyt", "glosn"
        );
        
        // --- KINESTETYK (KINESTHETIC) ---
        // Zawiera: ruch, dotyk, robienie rzeczy, emocje fizyczne
        Set<String> kinestheticWords = Set.of(
            "ruch", "rusz", "bieg", "chodz", "spacer", "sport", "cwicz", "taniec", "tanc",
            "dotyk", "czuj", "lapac", "trzym", "rek", "manual", "dloni", "palc",
            "rob", "dzial", "praktyk", "aktyw", "gest", "majster", "budow", "mont", "napraw",
            "kinest", "kinesthetic", "fizycz", "cial", "zmecz", "energ",
            "doswiadcz", "eksperyment", "warsztat", "model", "makieta"
        );

        int v = countWordsWithNegationCheck(normalizedText, visualWords, "VISUAL");
        int a = countWordsWithNegationCheck(normalizedText, auditoryWords, "AUDITORY");
        int k = countWordsWithNegationCheck(normalizedText, kinestheticWords, "KINESTHETIC");
        
        log.info("Wyniki słów: V={}, A={}, K={}", v, a, k);

        if (v > a && v > k) return LearningStyle.VISUAL;
        if (a > v && a > k) return LearningStyle.AUDITORY;
        if (k > v && k > a) return LearningStyle.KINESTHETIC;
        
        return LearningStyle.MIXED;
    }

    private int countWordsWithNegationCheck(String text, Set<String> keywords, String debugTag) {
        int count = 0;
        String[] words = text.split("[^\\p{L}]+"); 
        // Pełna lista negacji (rzeczowniki i czasowniki)
        Set<String> negations = Set.of(
            "nie", "bez", "brak", "zero", "zadn", 
            "nienawidz", "nienawis", "wstret", "brzydz", 
            "unika", "omija", 
            "nigdy", "wcale", "malo", "rzadko", "ani"
        );
        
        for (int i = 0; i < words.length; i++) {
            String word = words[i]; 
            
            // 1. Ignoruj słowa, które same w sobie są negacjami (np. "nienawidze" nie zaliczy "widze")
            if (negations.stream().anyMatch(word::contains)) continue;

            boolean match = keywords.stream().anyMatch(word::contains);
            
            if (match) {
                boolean hasNegation = false;
                // Sprawdź 3 słowa wstecz
                for (int j = Math.max(0, i - 3); j < i; j++) {
                    String prev = words[j];
                    if (negations.stream().anyMatch(neg -> prev.contains(neg))) {
                        hasNegation = true;
                        log.info("[{}] Ignoruję '{}' bo znaleziono negację '{}'", debugTag, word, prev);
                        break;
                    }
                }
                if (!hasNegation) {
                    count++;
                    log.info("[{}] Zaliczono: '{}'", debugTag, word);
                }
            }
        }
        return count;
    }

    private LearningStyle determineDominantStyle(Map<LearningStyle, Integer> scores) {
        List<Map.Entry<LearningStyle, Integer>> sortedScores = scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toList());

        Map.Entry<LearningStyle, Integer> first = sortedScores.get(0);
        Map.Entry<LearningStyle, Integer> second = sortedScores.get(1);

        if (first.getValue() == 0) return LearningStyle.MIXED;

        if (first.getValue().equals(second.getValue())) {
             if (first.getKey() == LearningStyle.KINESTHETIC || second.getKey() == LearningStyle.KINESTHETIC) return LearningStyle.KINESTHETIC;
             if (first.getKey() == LearningStyle.VISUAL || second.getKey() == LearningStyle.VISUAL) return LearningStyle.VISUAL;
             return first.getKey();
        }
        return first.getKey();
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
        try {
            User currentUser = getAuthenticatedUser();
            currentUser.setLearningStyle(style);
            userRepository.save(currentUser);
            updateAuthenticationObject(currentUser);
        } catch (Exception e) { }
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