package pl.learnedge.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.learnedge.dto.UpdateProfileDto;
import pl.learnedge.model.User;
import pl.learnedge.service.LearningStyleService;
import pl.learnedge.service.ProfilePictureService;
import pl.learnedge.service.UserService;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final UserService userService;
    private final ProfilePictureService profilePictureService;
    private final LearningStyleService learningStyleService;

    @GetMapping("/profil")
    public String profile(@AuthenticationPrincipal User user, Model model) {
        UpdateProfileDto profileDto = new UpdateProfileDto();
        profileDto.setFirstName(user.getFirstName());
        profileDto.setLastName(user.getLastName());
        
        // Tutaj był błąd, jeśli w serwisie brakowało metody isAiAnalysisAvailable()
        // Upewnij się, że dodałeś ją w Kroku 0
        boolean aiAvailable = learningStyleService.isAiAnalysisAvailable();
        
        model.addAttribute("profile", profileDto);
        model.addAttribute("aiAvailable", aiAvailable);
        return "dashboard/profile";
    }

    @PostMapping("/profil")
    public String updateProfile(@AuthenticationPrincipal User user,
                              UpdateProfileDto profileDto,
                              RedirectAttributes ra) {
        try {
            User updatedUser = userService.updateProfile(user.getId(), profileDto);
            
            // Aktualizacja sesji użytkownika
            var auth = new UsernamePasswordAuthenticationToken(
                updatedUser, user.getPassword(), user.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            ra.addFlashAttribute("success", "Profil został zaktualizowany.");
        } catch (Exception e) {
            log.error("Błąd aktualizacji profilu", e);
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profil";
    }

    @PostMapping("/api/profile/picture")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(@RequestParam("profilePicture") MultipartFile file) {
        try {
            // Używam loggera zamiast System.out.println dla czystości kodu
            log.info("Otrzymano plik: {}, rozmiar: {}, typ: {}", 
                    file.getOriginalFilename(), file.getSize(), file.getContentType());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Plik jest pusty"));
            }
            
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Nieprawidłowy format pliku. Dozwolone są tylko obrazy."));
            }
            
            String imageUrl = profilePictureService.saveProfilePicture(file);
            log.info("Zdjęcie zapisane: {}", imageUrl);
            
            return ResponseEntity.ok(Map.of("url", imageUrl));
        } catch (Exception e) {
            log.error("Błąd uploadu zdjęcia", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Wystąpił błąd podczas przesyłania zdjęcia: " + e.getMessage()));
        }
    }
}