package pl.learnedge.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class CookieConsentService {
    
    private static final String CONSENT_COOKIE_NAME = "cookie_consent";
    private static final String PREFERENCES_COOKIE_NAME = "cookie_preferences";
    private static final int CONSENT_COOKIE_AGE = 365 * 24 * 60 * 60; // 1 rok
    
    public boolean hasConsent(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (CONSENT_COOKIE_NAME.equals(cookie.getName())) {
                    return "accepted".equals(cookie.getValue());
                }
            }
        }
        return false;
    }
    
    public void setConsent(HttpServletResponse response, boolean accepted) {
        Cookie consentCookie = new Cookie(CONSENT_COOKIE_NAME, accepted ? "accepted" : "rejected");
        consentCookie.setMaxAge(CONSENT_COOKIE_AGE);
        consentCookie.setPath("/");
        consentCookie.setHttpOnly(true);
        consentCookie.setSecure(false); // Ustaw na true w produkcji z HTTPS
        response.addCookie(consentCookie);
    }
    
    public void setPreferences(HttpServletResponse response, String preferences) {
        if (preferences != null && !preferences.isEmpty()) {
            Cookie prefCookie = new Cookie(PREFERENCES_COOKIE_NAME, preferences);
            prefCookie.setMaxAge(CONSENT_COOKIE_AGE);
            prefCookie.setPath("/");
            prefCookie.setHttpOnly(false); // Dostępne dla JavaScript
            prefCookie.setSecure(false); // Ustaw na true w produkcji z HTTPS
            response.addCookie(prefCookie);
        }
    }
    
    public void clearCookies(HttpServletResponse response) {
        // Usuń cookie zgody
        Cookie consentCookie = new Cookie(CONSENT_COOKIE_NAME, "");
        consentCookie.setMaxAge(0);
        consentCookie.setPath("/");
        response.addCookie(consentCookie);
        
        // Usuń cookie preferencji
        Cookie prefCookie = new Cookie(PREFERENCES_COOKIE_NAME, "");
        prefCookie.setMaxAge(0);
        prefCookie.setPath("/");
        response.addCookie(prefCookie);
    }
}