package pl.learnedge.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.learnedge.service.CookieConsentService;

import java.util.Map;

@RestController
@RequestMapping("/api/cookies")
public class CookieConsentController {

    private final CookieConsentService cookieConsentService;

    @Autowired
    public CookieConsentController(CookieConsentService cookieConsentService) {
        this.cookieConsentService = cookieConsentService;
    }

    @PostMapping("/consent")
    public ResponseEntity<Map<String, String>> setConsent(
            @RequestParam boolean accepted,
            @RequestParam(required = false) String preferences,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        cookieConsentService.setConsent(response, accepted);
        
        if (accepted && preferences != null) {
            cookieConsentService.setPreferences(response, preferences);
        }
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", accepted ? "Zgoda na cookies została udzielona" : "Zgoda na cookies została odrzucona"
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getConsentStatus(HttpServletRequest request) {
        boolean hasConsent = cookieConsentService.hasConsent(request);
        
        return ResponseEntity.ok(Map.of(
            "hasConsent", hasConsent,
            "showBanner", !hasConsent
        ));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCookies(HttpServletResponse response) {
        cookieConsentService.clearCookies(response);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Cookies zostały usunięte"
        ));
    }
}