package pl.learnedge.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pl.learnedge.service.LearningStyleService;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AiSurveyController {

    private final LearningStyleService learningStyleService;

    @GetMapping("/ankieta")
    public String showSurvey(Model model) {
        // Sprawdź czy AI jest dostępne
        boolean aiAvailable = learningStyleService.isAiAnalysisAvailable();
        model.addAttribute("aiAvailable", aiAvailable);

        if (!aiAvailable) {
            model.addAttribute("message", "Analiza AI jest obecnie niedostępna. Możesz wybrać styl uczenia ręcznie.");
        }

        return "dashboard/ankieta";
    }

    @PostMapping("/api/survey/analyze")
    @ResponseBody
    public ResponseEntity<?> analyzeSurvey(@RequestBody Map<String, String> surveyAnswers) {
        try {
            log.info("Otrzymano ankietę z {} odpowiedziami", surveyAnswers.size());
            
            // Walidacja danych wejściowych
            if (surveyAnswers == null || surveyAnswers.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Brak odpowiedzi w ankiecie"));
            }
            
            // Analizuj odpowiedzi (automatycznie wybierze AI lub algorytm alternatywny)
            String learningStyle = learningStyleService.analyzeAndSaveLearningStyle(surveyAnswers);

            boolean aiUsed = learningStyleService.isAiAnalysisAvailable();
            String analysisMethod = aiUsed ? "AI" : "algorytmem alternatywnym";

            log.info("Pomyślnie przeanalizowano styl uczenia: {} (metoda: {})", learningStyle, analysisMethod);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "learningStyle", learningStyle,
                "message", "Twój styl uczenia został przeanalizowany " + analysisMethod + ": " + translateStyle(learningStyle)
            ));

        } catch (Exception e) {
            log.error("Błąd podczas analizy ankiety: ", e);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Wystąpił błąd podczas analizy"));
        }
    }

    // Manual selection endpoint removed - users must complete survey when AI is unavailable

    private String translateStyle(String style) {
        return switch (style) {
            case "VISUAL" -> "Wzrokowy";
            case "AUDITORY" -> "Słuchowy";
            case "KINESTHETIC" -> "Kinestetyczny";
            case "MIXED" -> "Mieszany";
            default -> style;
        };
    }
}