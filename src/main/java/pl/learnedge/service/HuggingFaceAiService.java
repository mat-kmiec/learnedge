package pl.learnedge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceAiService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final List<String> LEARNING_STYLE_LABELS = List.of("VISUAL", "AUDITORY", "KINESTHETIC");

    @Value("${HF_TOKEN:}")
    private String tokenPart1;

    @Value("${HF_TOKEN2:}")
    private String tokenPart2;

    @Value("${app.ai.huggingface.model}")
    private String model;

    @Value("${app.ai.huggingface.api-url}")
    private String apiUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        String token = getFullToken();
        log.info("=== Konfiguracja Hugging Face AI Service ===");
        log.info("Token: {}", token != null && !token.isEmpty() ? token.substring(0, Math.min(10, token.length())) + "..." : "BRAK");
        log.info("Model: {}", model);
        log.info("API URL: {}", apiUrl);
        log.info("AI Service zainicjalizowany - sprawdzanie dostępności zostanie wykonane przy pierwszym użyciu");
    }

    /**
     * Łączy dwie części tokena
     */
    private String getFullToken() {
        if ((tokenPart1 == null || tokenPart1.isEmpty()) && (tokenPart2 == null || tokenPart2.isEmpty())) {
            return "";
        }
        return (tokenPart1 != null ? tokenPart1 : "") + (tokenPart2 != null ? tokenPart2 : "");
    }

    /**
     * Analizuje odpowiedzi ankiety i określa styl uczenia się
     */
    public String analyzeLearningStyle(Map<String, String> surveyAnswers) {
        try {
            // Najpierw spróbuj analizy słów kluczowych z ankiety (pytania wielokrotnego wyboru)
            String keywordResult = analyzeByKeywords(surveyAnswers);
            
            // Jeśli są odpowiedzi z ankiety i dały wynik - użyj go
            if (!"MIXED".equals(keywordResult)) {
                log.info("Analiza słów kluczowych z ankiety wskazała: {}", keywordResult);
                return keywordResult;
            }

            // Jeśli nie ma odpowiedzi z ankiety LUB wynik jest MIXED, spróbuj analizę opisu tekstowego
            String userDescription = surveyAnswers.get("userDescription");
            if (userDescription != null && !userDescription.trim().isEmpty()) {
                log.info("Brak jednoznacznych odpowiedzi z ankiety, analizuję opis tekstowy");
                
                // Najpierw spróbuj analizę słów kluczowych w opisie (bardziej niezawodne niż API)
                String textAnalysisResult = analyzeTextByKeywords(userDescription);
                if (!"MIXED".equals(textAnalysisResult)) {
                    log.info("Analiza słów kluczowych w opisie wskazała: {}", textAnalysisResult);
                    return textAnalysisResult;
                }
                
                // Jeśli analiza słów kluczowych nie dała wyniku, spróbuj API jako ostateczność
                try {
                    String prompt = buildAnalysisPrompt(surveyAnswers);
                    String aiResult = callHuggingFaceApi(prompt);
                    log.info("Analiza AI wskazała: {}", aiResult);
                    return extractLearningStyle(aiResult);
                } catch (Exception e) {
                    log.warn("API AI niedostępne, używam wyniku analizy tekstowej: {}", textAnalysisResult);
                    return textAnalysisResult; // Zwróć MIXED jeśli API nie działa
                }
            }

            // Jeśli nic nie zadziałało, zwróć MIXED
            log.info("Brak wystarczających danych do analizy, zwracam MIXED");
            return "MIXED";
        } catch (Exception e) {
            log.error("Błąd podczas analizy stylu uczenia: ", e);
            return "MIXED"; // Fallback
        }
    }

    private String analyzeByKeywords(Map<String, String> answers) {
        int visualScore = 0;
        int auditoryScore = 0;
        int kinestheticScore = 0;

        // Zlicz odpowiedzi - wartości z ankiety to "visual", "auditory", "kinesthetic"
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Pomiń pole userDescription (opis tekstowy)
            if ("userDescription".equals(key)) {
                continue;
            }
            
            String normalizedAnswer = value.toLowerCase().trim();
            if (normalizedAnswer.equals("visual")) {
                visualScore++;
            } else if (normalizedAnswer.equals("auditory")) {
                auditoryScore++;
            } else if (normalizedAnswer.equals("kinesthetic")) {
                kinestheticScore++;
            }
        }

        log.info("Wyniki analizy ankiety - Visual: {}, Auditory: {}, Kinesthetic: {}",
                visualScore, auditoryScore, kinestheticScore);

        // Określenie wyniku na podstawie najwyższego wyniku
        if (visualScore > auditoryScore && visualScore > kinestheticScore) {
            return "VISUAL";
        } else if (auditoryScore > visualScore && auditoryScore > kinestheticScore) {
            return "AUDITORY";
        } else if (kinestheticScore > visualScore && kinestheticScore > auditoryScore) {
            return "KINESTHETIC";
        }

        // Jeśli wyniki są równe, zwróć styl dominujący lub MIXED
        int maxScore = Math.max(visualScore, Math.max(auditoryScore, kinestheticScore));
        if (maxScore == 0) {
            return "MIXED"; // Brak odpowiedzi na pytania wielokrotnego wyboru
        }
        
        // W przypadku remisu, wybierz pierwszy dominujący styl
        if (visualScore == maxScore) {
            return "VISUAL";
        } else if (auditoryScore == maxScore) {
            return "AUDITORY";
        } else {
            return "KINESTHETIC";
        }
    }

    /**
     * Analizuje opis tekstowy pod kątem słów kluczowych związanych ze stylami uczenia
     */
    private String analyzeTextByKeywords(String text) {
        String lowerText = text.toLowerCase();
        
        int visualScore = 0;
        int auditoryScore = 0;
        int kinestheticScore = 0;
        
        // Słowa kluczowe VISUAL (wzrokowy)
        String[] visualKeywords = {
            "zobaczyć", "widzieć", "obraz", "wygląd", "patrz", "obserwuj",
            "schemat", "diagram", "mapa", "rysunek", "ilustracja", "zdjęcie",
            "kolory", "jasny", "ciemny", "wzór", "kształt", "film", "video",
            "czytać", "książk", "tekst", "napisać", "notatk"
        };
        
        // Słowa kluczowe AUDITORY (słuchowy)
        String[] auditoryKeywords = {
            "słysz", "słuch", "dźwięk", "głos", "mówić", "rozmawia",
            "dyskusj", "wyjaśni", "opowiedz", "muzyk", "podcast", "audio",
            "rytm", "melodia", "cisza", "hałas", "ton", "brzmi",
            "na głos", "powtarzać", "słowami"
        };
        
        // Słowa kluczowe KINESTHETIC (kinestetyczny)
        String[] kinestheticKeywords = {
            "poczuć", "czuć", "czuj", "dotknąć", "dotyk", "ruch", "porusz",
            "działa", "działanie", "praktyk", "ćwiczy", "robic", "wykonać",
            "ręk", "rączk", "chwyc", "trzyma", "doświadcz", "spróbuj",
            "aktywn", "sport", "wejść", "ruszyć", "krok", "energia",
            "napięcie", "fizyczn", "smakuje", "temperatur"
        };
        
        // Zlicz wystąpienia słów kluczowych
        for (String keyword : visualKeywords) {
            if (lowerText.contains(keyword)) {
                visualScore++;
            }
        }
        
        for (String keyword : auditoryKeywords) {
            if (lowerText.contains(keyword)) {
                auditoryScore++;
            }
        }
        
        for (String keyword : kinestheticKeywords) {
            if (lowerText.contains(keyword)) {
                kinestheticScore++;
            }
        }
        
        log.info("Wyniki analizy tekstu - Visual: {}, Auditory: {}, Kinesthetic: {}",
                visualScore, auditoryScore, kinestheticScore);
        
        // Określenie wyniku na podstawie najwyższego wyniku (wymagamy przynajmniej 3 słowa kluczowe)
        int maxScore = Math.max(visualScore, Math.max(auditoryScore, kinestheticScore));
        
        if (maxScore < 3) {
            log.info("Zbyt mało słów kluczowych w tekście (max: {}), zwracam MIXED", maxScore);
            return "MIXED";
        }
        
        // Jeśli różnica między najwyższym a drugim jest za mała, zwróć MIXED
        int secondMax = 0;
        if (visualScore != maxScore) secondMax = Math.max(secondMax, visualScore);
        if (auditoryScore != maxScore) secondMax = Math.max(secondMax, auditoryScore);
        if (kinestheticScore != maxScore) secondMax = Math.max(secondMax, kinestheticScore);
        
        if (maxScore - secondMax < 2) {
            log.info("Zbyt mała różnica między stylami ({} vs {}), zwracam MIXED", maxScore, secondMax);
            return "MIXED";
        }
        
        // Określ dominujący styl
        if (visualScore == maxScore) {
            return "VISUAL";
        } else if (auditoryScore == maxScore) {
            return "AUDITORY";
        } else if (kinestheticScore == maxScore) {
            return "KINESTHETIC";
        }
        
        return "MIXED";
    }

    private String buildAnalysisPrompt(Map<String, String> answers) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this person's learning style and determine if they are: VISUAL (prefer images, diagrams, reading), AUDITORY (prefer listening, discussions, explanations), or KINESTHETIC (prefer hands-on, movement, practice) learner.\n\n");

        // Sprawdź czy są odpowiedzi z ankiety
        boolean hasQuestionAnswers = answers.entrySet().stream()
            .anyMatch(e -> e.getKey().startsWith("question"));
        
        if (hasQuestionAnswers) {
            // Jeśli są odpowiedzi z ankiety, uwzględnij je
            prompt.append("Survey answers:\n");
            answers.forEach((key, value) -> {
                if (key.startsWith("question")) {
                    prompt.append("- ").append(value).append("\n");
                }
            });
            prompt.append("\n");
        }
        
        // Zawsze dodaj opis tekstowy jeśli istnieje
        String userDescription = answers.get("userDescription");
        if (userDescription != null && !userDescription.trim().isEmpty()) {
            prompt.append("Personal description:\n").append(userDescription).append("\n\n");
        }

        prompt.append("Based on this information, this person is primarily a:");
        return prompt.toString();
    }

    private String callHuggingFaceApi(String prompt) {
        log.info("Wysyłam prompt do Hugging Face API: {}", prompt);

        // Poprawny URL: https://api-inference.huggingface.co/models/facebook/bart-large-mnli
        String fullUrl = apiUrl.endsWith("/") ? apiUrl + model : apiUrl + "/" + model;
        String token = getFullToken();
        
        WebClient webClient = webClientBuilder
                .baseUrl(fullUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();

        Map<String, Object> requestBody = Map.of(
            "inputs", prompt,
            "parameters", Map.of(
                "candidate_labels", LEARNING_STYLE_LABELS,
                "multi_label", false
            )
        );

        log.info("Request URL: {}", fullUrl);
        log.info("Request body: {}", requestBody);

        try {
            String rawResponse = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(REQUEST_TIMEOUT);

            log.info("Odpowiedź z Hugging Face API: {}", rawResponse);

            if (rawResponse == null || rawResponse.isBlank()) {
                log.warn("Pusta odpowiedź z Hugging Face API");
                return "MIXED";
            }

            JsonNode responseNode = objectMapper.readTree(rawResponse);

            if (responseNode.has("error")) {
                log.warn("Hugging Face zwróciło błąd: {}", responseNode.get("error").asText());
                return "MIXED";
            }

            JsonNode resultNode = responseNode.isArray() && responseNode.size() > 0
                    ? responseNode.get(0)
                    : responseNode;

            JsonNode labelsNode = resultNode.get("labels");
            JsonNode scoresNode = resultNode.get("scores");

            if (labelsNode != null && labelsNode.isArray() && labelsNode.size() > 0) {
                if (scoresNode != null && scoresNode.isArray()) {
                    log.info("Labels: {}, Scores: {}", labelsNode, scoresNode);
                }
                return labelsNode.get(0).asText();
            }

            log.warn("Nieprawidłowa odpowiedź z API - brak pola 'labels'");
            return "MIXED";
        } catch (WebClientResponseException e) {
            log.error("Błąd HTTP podczas wywołania Hugging Face API (status {}): {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return "MIXED";
        } catch (Exception e) {
            log.error("Błąd wywołania Hugging Face API: ", e);
            return "MIXED";
        }
    }

    private String extractLearningStyle(String apiResult) {
        // Dla wyników z nowego API (bezpośrednio etykieta)
        if (apiResult.toUpperCase().contains("VISUAL")) {
            return "VISUAL";
        } else if (apiResult.toUpperCase().contains("AUDITORY")) {
            return "AUDITORY";
        } else if (apiResult.toUpperCase().contains("KINESTHETIC")) {
            return "KINESTHETIC";
        }
        return "MIXED";
    }

    /**
     * Sprawdza czy usługa AI jest dostępna
     * UWAGA: Obecnie zawsze zwraca true - ankieta używa algorytmu słów kluczowych jako głównej metody
     */
    public boolean isAvailable() {
        // Zwracamy true - algorytm słów kluczowych jest wystarczający
        // API Hugging Face jest używane tylko jako opcjonalny backup
        return true;
    }
}