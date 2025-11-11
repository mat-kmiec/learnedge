package pl.learnedge.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.learnedge.dto.ChangePasswordRequest;
import pl.learnedge.model.User;
import pl.learnedge.service.EmailService;
import pl.learnedge.service.PasswordService;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final EmailService mailService;
    private final PasswordService passwordService;

    @GetMapping("/przypomnij-haslo")
    public String forgotPassword(@RequestParam(value = "sent", required = false) String sent,
                                 Model model) {
        model.addAttribute("sent", sent != null);
        return "home/forgot-password";
    }

    @PostMapping("/przypomnij-haslo")
    public String sendReset(@RequestParam String email,
                            HttpServletRequest request,
                            RedirectAttributes ra) {
        try {
            User user = passwordService.requireUserByEmail(email);
            String token = passwordService.createTokenForUser(user, 30);
            String link = getAppUrl(request) + "/reset-hasla?token=" + token;
            mailService.send(email, "Reset hasła – LearnEdge",
                    "Cześć!\n\nKliknij, aby ustawić nowe hasło:\n" + link + "\n");
        } catch (Exception ignored) {

        }
        ra.addAttribute("sent", "true");
        return "redirect:/przypomnij-haslo";
    }

    @GetMapping("/reset-hasla")
    public String resetForm(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        return "home/reset-password";
    }

    @PostMapping("/reset-hasla")
    public String doReset(@RequestParam String token,
                          @RequestParam String password,
                          RedirectAttributes ra) {
        try {
            passwordService.resetPassword(token, password);
            ra.addFlashAttribute("info", "Hasło zmienione. Zaloguj się nowym hasłem.");
            return "redirect:/logowanie";
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/reset-hasla?token=" + token;
        }
    }

    @PostMapping("/api/profile/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            passwordService.changePassword(request.getCurrentPassword(), request.getNewPassword());
            Map<String, String> body = new HashMap<>();
            body.put("success", "Hasło zostało zmienione");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            log.warn("Change password validation error: {}", e.getMessage());
            Map<String, String> body = new HashMap<>();
            body.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            log.error("Change password error: ", e);
            Map<String, String> body = new HashMap<>();
            body.put("error", "Wystąpił błąd podczas zmiany hasła");
            return ResponseEntity.internalServerError().body(body);
        }
    }


    private String getAppUrl(HttpServletRequest req) {
        String scheme = req.getHeader("X-Forwarded-Proto");
        if (scheme == null) scheme = req.getScheme();
        String host = req.getHeader("X-Forwarded-Host");
        if (host == null) {
            int port = req.getServerPort();
            String portPart = (port == 80 || port == 443) ? "" : ":" + port;
            host = req.getServerName() + portPart;
        }
        return scheme + "://" + host;
    }
}
