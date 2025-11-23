package pl.learnedge.controller;

// ...istniejące importy...
import pl.learnedge.model.User;
import pl.learnedge.service.LearningStyleService;
// Importujemy wewnętrzną klasę z wynikiem
import pl.learnedge.service.LearningStyleService.AnalysisResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AiSurveyController {
    private final LearningStyleService learningStyleService;

    @PostMapping("/api/survey/set-style")
    @ResponseBody
    public ResponseEntity<?> setLearningStyle(@RequestBody Map<String, String> payload) {
        String style = payload.get("style");
        if (style == null || style.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Brak stylu do ustawienia"));
        }
        try {
            User currentUser = learningStyleService.getAuthenticatedUser();
            currentUser.setLearningStyle(style.toUpperCase());
            learningStyleService.getUserRepository().save(currentUser);
            learningStyleService.updateAuthenticationObject(currentUser);
            return ResponseEntity.ok(Map.of("success", true, "style", style.toUpperCase()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Nie udało się ustawić stylu"));
        }
    }

    @GetMapping("/ankieta")
    public String showSurvey(Model model) {
        boolean aiAvailable = learningStyleService.isAiAnalysisAvailable();
        model.addAttribute("aiAvailable", aiAvailable);

        if (!aiAvailable) {
            model.addAttribute("message", "Analiza AI jest obecnie niedostępna. Wypełnij ankietę, która zostanie przeanalizowana algorytmem alternatywnym.");
        }

        return "dashboard/ankieta";
    }

    @PostMapping("/api/survey/analyze")
    @ResponseBody
    public ResponseEntity<?> analyzeSurvey(@RequestBody Map<String, String> surveyAnswers) {
        try {
            log.info("Otrzymano ankietę z {} odpowiedziami", surveyAnswers.size());

            // Walidacja
            if (surveyAnswers == null || surveyAnswers.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Brak odpowiedzi w ankiecie"));
            }

            // 1. Wywołujemy nową metodę serwisu, która zwraca obiekt AnalysisResult
            AnalysisResult result = learningStyleService.analyzeAndSaveLearningStyle(surveyAnswers);
            
            // 2. Wyciągamy dane z obiektu wyniku
            String learningStyle = result.getDominantStyle();
            Map<String, Double> stylePercents = result.getPercentages();
            
            boolean aiUsed = learningStyleService.isAiAnalysisAvailable();
            String analysisMethod = aiUsed ? "AI" : "algorytmem alternatywnym";
            
            log.info("Pomyślnie przeanalizowano styl uczenia: {} (metoda: {})", learningStyle, analysisMethod);

            // 3. Zwracamy odpowiedź używając danych z result
            return ResponseEntity.ok(Map.of(
                "success", true,
                "learningStyle", learningStyle,
                "message", "Twój styl uczenia został przeanalizowany " + analysisMethod + ": " + translateStyle(learningStyle),
                "percents", stylePercents
            ));

        } catch (Exception e) {
            log.error("Błąd podczas analizy ankiety: ", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Wystąpił błąd podczas analizy: " + e.getMessage()));
        }
    }

    private String translateStyle(String style) {
        if (style == null) return "Nieokreślony";
        return switch (style.toUpperCase()) {
            case "VISUAL" -> "Wzrokowy";
            case "AUDITORY" -> "Słuchowy";
            case "KINESTHETIC" -> "Kinestetyczny";
            case "MIXED" -> "Mieszany";
            default -> style;
        };
    }
}