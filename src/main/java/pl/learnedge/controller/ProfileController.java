package pl.learnedge.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class ProfileController {

    private final UserService userService;
    private final ProfilePictureService profilePictureService;
    private final LearningStyleService learningStyleService;

    @GetMapping("/profil")
    public String profile(@AuthenticationPrincipal User user, Model model) {
        UpdateProfileDto profileDto = new UpdateProfileDto();
        profileDto.setFirstName(user.getFirstName());
        profileDto.setLastName(user.getLastName());
        
        // Sprawdź dostępność AI
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
            // Update the authentication object
            var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                updatedUser, user.getPassword(), user.getAuthorities()
            );
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            ra.addFlashAttribute("success", "Profil został zaktualizowany.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profil";
    }
    @PostMapping("/api/profile/picture")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(@RequestParam("profilePicture") MultipartFile file) {
        try {
            System.out.println("Received file upload request");
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            System.out.println("Content type: " + file.getContentType());

            if (file.isEmpty()) {
                System.out.println("File is empty");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Plik jest pusty"));
            }
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                System.out.println("Invalid content type: " + contentType);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Nieprawidłowy format pliku. Dozwolone są tylko obrazy."));
            }
            String imageUrl = profilePictureService.saveProfilePicture(file);
            System.out.println("File saved successfully. URL: " + imageUrl);
            return ResponseEntity.ok(Map.of("url", imageUrl));
        } catch (Exception e) {
            e.printStackTrace(); // Log the error
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Wystąpił błąd podczas przesyłania zdjęcia: " + e.getMessage()));
        }
    }
}
