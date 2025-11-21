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

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    
    // Czyste etykiety (bez nawiasów) działają lepiej przy negacjach
    private static final List<String> CANDIDATE_LABELS = List.of(
        "Visual learner", 
        "Auditory learner", 
        "Kinesthetic learner"
    );

    @Value("${HF_TOKEN:}")
    private String tokenPart1;

    @Value("${HF_TOKEN2:}")
    private String tokenPart2;

    @Value("${app.ai.huggingface.model:facebook/bart-large-mnli}")
    private String model;

    @Value("${app.ai.huggingface.api-url:https://api-inference.huggingface.co/models/}")
    private String apiUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        log.info("=== Hugging Face AI Service (Context Injection) ===");
        log.info("Model: {}", model);
    }

    private String getFullToken() {
        return (tokenPart1 != null ? tokenPart1 : "") + (tokenPart2 != null ? tokenPart2 : "");
    }

    public String analyzeLearningStyle(Map<String, String> surveyAnswers) {
        String userDescription = surveyAnswers.get("userDescription");

        if (userDescription == null || userDescription.trim().length() < 3) {
            log.warn("Tekst do analizy AI jest pusty.");
            return "MIXED";
        }

        // --- CONTEXT INJECTION ---
        // Model jest po angielsku. Polskie metafory ("mam przed oczami") są dla niego niezrozumiałe.
        // Skanujemy tekst w poszukiwaniu słów kluczowych i dodajemy angielskie wskazówki.
        String textToAnalyze = injectContext(userDescription);

        try {
            return callHuggingFaceApi(textToAnalyze);
        } catch (Exception e) {
            log.error("AI Analysis failed: {}", e.getMessage());
            return "MIXED";
        }
    }

    private String injectContext(String text) {
        StringBuilder context = new StringBuilder();
        String lower = text.toLowerCase();

        // 1. Wzrokowiec
        // Unikamy słów, które łatwo zanegować ("czytać", "patrzeć"), skupiamy się na rzeczownikach i silnych deklaracjach
        if (lower.contains("wzrok") || lower.contains("obraz") || lower.contains("schemat") || 
            lower.contains("wykres") || lower.contains("kolor") || lower.contains("ilustracja") || 
            lower.contains("diagram") || lower.contains("widz")) {
            context.append(" visual learning style. images. diagrams. seeing. ");
        }

        // 2. Słuchowiec
        if (lower.contains("słuch") || lower.contains("sluch") || lower.contains("dźwięk") || 
            lower.contains("dzwiek") || lower.contains("głos") || lower.contains("podcast") || 
            lower.contains("nagran") || lower.contains("muzyk") || lower.contains("rozmaw") || 
            lower.contains("dyskut")) {
            context.append(" auditory learning style. listening. sound. discussion. ");
        }

        // 3. Kinestetyk
        if (lower.contains("kinest") || lower.contains("ruch") || lower.contains("dotyk") || 
            lower.contains("ciał") || lower.contains("praktyk") || lower.contains("robic") || 
            lower.contains("budow") || lower.contains("sport")) {
            context.append(" kinesthetic learning style. movement. touching. doing. ");
        }

        // Doklejamy kontekst na końcu, aby model wziął go pod uwagę
        if (context.length() > 0) {
            return text + " [Context clues:" + context.toString() + "]";
        }
        
        return text;
    }

    private String callHuggingFaceApi(String textInput) {
        String fullUrl = apiUrl.endsWith("/") ? apiUrl + model : apiUrl + "/" + model;
        if (!fullUrl.contains("api-inference.huggingface.co")) {
             fullUrl = "https://api-inference.huggingface.co/models/" + model;
        }

        String token = getFullToken();
        
        WebClient webClient = webClientBuilder
                .baseUrl(fullUrl)
                .defaultHeader("Authorization", "Bearer " + token)
                .build();

        Map<String, Object> requestBody = Map.of(
            "inputs", textInput,
            "parameters", Map.of(
                "candidate_labels", CANDIDATE_LABELS,
                "multi_label", false
            )
        );

        log.info("Wysyłam do AI: '{}'", textInput);

        try {
            String rawResponse = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(REQUEST_TIMEOUT);

            return parseResponse(rawResponse);

        } catch (WebClientResponseException e) {
            log.error("Błąd API HF: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "MIXED";
        } catch (Exception e) {
            log.error("Błąd połączenia z AI", e);
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
                    
                    log.info("AI zdecydowało: {} (pewność: {})", winner, confidence);
                    
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