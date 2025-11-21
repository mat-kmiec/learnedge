package pl.learnedge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HuggingFaceAiService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(45);
    
    private static final List<String> CANDIDATE_LABELS = List.of(
        "Visual learner", 
        "Auditory learner", 
        "Kinesthetic learner",
        "Negative sentiment, hate or denial"
    );

    @Value("${HF_TOKEN:}")
    private String tokenPart1;

    @Value("${HF_TOKEN2:}")
    private String tokenPart2;

    @Value("${app.ai.huggingface.model:facebook/bart-large-mnli}")
    private String classificationModel;

    private final String translationModel = "Helsinki-NLP/opus-mt-pl-en";

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("=== Hugging Face AI Service: TRANSLATOR + BART (URL FIX) ===");
    }

    private String getFullToken() {
        return (tokenPart1 != null ? tokenPart1 : "") + (tokenPart2 != null ? tokenPart2 : "");
    }

    public String analyzeLearningStyle(Map<String, String> surveyAnswers) {
        String userDescription = surveyAnswers.get("userDescription");

        if (userDescription == null || userDescription.trim().length() < 4) {
            return "MIXED";
        }

        try {
            String englishText = translateToEnglish(userDescription);
            if (englishText == null) return "MIXED";
            return classifyEnglishText(englishText);
        } catch (Exception e) {
            log.error("AI Chain failed: {}", e.getMessage());
            return "MIXED";
        }
    }

    private String translateToEnglish(String polishText) {
        // FIX: Próbujemy użyć standardowego API Inference dla tłumacza, 
        // bo Router często zwraca 404 dla starszych modeli jak Helsinki-NLP
        String url = "https://api-inference.huggingface.co/models/" + translationModel;
        String token = getFullToken();

        WebClient webClient = webClientBuilder
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();

        Map<String, String> requestBody = Map.of("inputs", polishText);

        try {
            String rawResponse = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(REQUEST_TIMEOUT);

            JsonNode root = objectMapper.readTree(rawResponse);
            if (root.isArray() && root.size() > 0 && root.get(0).has("translation_text")) {
                String translated = root.get(0).get("translation_text").asText();
                log.info("Tłumaczenie: '{}' -> '{}'", polishText, translated);
                return translated;
            }
            return null;
        } catch (Exception e) {
            log.warn("Tłumaczenie nieudane (błąd API): {}", e.getMessage());
            return null;
        }
    }

    private String classifyEnglishText(String englishText) {
        // Router zazwyczaj działa dobrze dla BART-a
        String url = "https://router.huggingface.co/models/" + classificationModel;
        String token = getFullToken();

        WebClient webClient = webClientBuilder
                .baseUrl(url)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();

        Map<String, Object> requestBody = Map.of(
            "inputs", englishText,
            "parameters", Map.of(
                "candidate_labels", CANDIDATE_LABELS,
                "multi_label", false
            )
        );

        try {
            String rawResponse = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(REQUEST_TIMEOUT);

            return parseResponse(rawResponse);
        } catch (Exception e) {
            log.error("Błąd klasyfikacji: {}", e.getMessage());
            return "MIXED";
        }
    }

    private String parseResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode result = root.isArray() ? root.get(0) : root;

            if (result.has("labels") && result.has("scores")) {
                JsonNode labels = result.get("labels");
                JsonNode scores = result.get("scores");
                
                if (labels.size() > 0) {
                    String winner = labels.get(0).asText();
                    double confidence = scores.get(0).asDouble();
                    
                    log.info("BART Decyzja: '{}' ({:.2f})", winner, confidence);
                    
                    if (winner.contains("Negative") || winner.contains("hate") || winner.contains("denial")) {
                        return "NEGATIVE";
                    }

                    if (confidence < 0.30) return "MIXED";

                    if (winner.contains("Visual")) return "VISUAL";
                    if (winner.contains("Auditory")) return "AUDITORY";
                    if (winner.contains("Kinesthetic")) return "KINESTHETIC";
                }
            }
            return "MIXED";
        } catch (Exception e) {
            log.error("Błąd parsowania JSON z AI", e);
            return "MIXED";
        }
    }

    public boolean isAvailable() {
        return !getFullToken().isEmpty();
    }
}