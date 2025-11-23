package pl.learnedge.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Service;
import pl.learnedge.model.User;
import pl.learnedge.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Użytkownik nie jest zalogowany");
        }
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof User user) {
            return user;
        }
        
        if (principal instanceof DefaultOidcUser oidcUser) {
            String email = oidcUser.getEmail();
            // ZMIANA: Zamiast tylko orElseGet, używamy mapowania, żeby zaktualizować istniejącego
            return userRepository.findByEmail(email)
                    .map(existingUser -> updateExistingUser(existingUser, oidcUser)) // Aktualizuj jeśli istnieje
                    .orElseGet(() -> registerNewGoogleUser(oidcUser)); // Stwórz jeśli nowy
        }
        
        throw new IllegalStateException("Nieznany typ użytkownika: " + principal.getClass().getName());
    }

    // Nowa metoda do aktualizacji danych przy logowaniu
    private User updateExistingUser(User user, DefaultOidcUser oidcUser) {
        boolean changed = false;

        // Pobierz URL zdjęcia z Google
        Object pictureObj = oidcUser.getAttributes().get("picture");
        
        // Jeśli użytkownik NIE MA zdjęcia w bazie, a Google je ma -> ustawiamy
        if (user.getProfilePicture() == null && pictureObj instanceof String pictureUrl) {
            user.setProfilePicture(pictureUrl);
            changed = true;
        }
        
        // Opcjonalnie: Możesz wymusić nadpisywanie zdjęcia z Google zawsze:
        // if (pictureObj instanceof String pictureUrl && !pictureUrl.equals(user.getProfilePicture())) {
        //     user.setProfilePicture(pictureUrl);
        //     changed = true;
        // }

        return changed ? userRepository.save(user) : user;
    }

    private User registerNewGoogleUser(DefaultOidcUser oidcUser) {
        User newUser = new User();
        String email = oidcUser.getEmail();
        newUser.setEmail(email);
        newUser.setUsername(email);
        newUser.setRole("ROLE_USER");
        newUser.setEnabled(true);
        newUser.setPassword("{bcrypt}" + java.util.UUID.randomUUID().toString());

        // Always set default profile picture for Google users
        newUser.setProfilePicture("https://cdn-icons-png.flaticon.com/512/149/149071.png");

        // Imię
        String firstName = null;
        if (email != null && email.contains("@")) {
            firstName = email.substring(0, email.indexOf("@"));
        }
        newUser.setFirstName(firstName);

        // Nazwisko
        Object familyNameObj = oidcUser.getAttributes().get("family_name");
        if (familyNameObj instanceof String familyName && !familyName.isBlank()) {
            newUser.setLastName(familyName);
        } else {
            newUser.setLastName(null);
        }

        return userRepository.save(newUser);
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}