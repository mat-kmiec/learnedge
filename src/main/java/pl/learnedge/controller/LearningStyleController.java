package pl.learnedge.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.learnedge.service.LearningStyleService;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class LearningStyleController {

    private final LearningStyleService learningStyleService;

    @PostMapping("/learning-style")
    public ResponseEntity<Map<String, String>> saveLearningStyle(@RequestBody Map<String, String> request) {
        // Metoda została wyłączona - styl uczenia można ustawić tylko poprzez ankietę AI
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Styl uczenia się można ustawić wyłącznie poprzez wypełnienie ankiety AI. Przejdź do zakładki 'Ankieta AI'."));
    }


}
