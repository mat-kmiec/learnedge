package pl.learnedge.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.learnedge.dto.UpdateProfileDto;
import pl.learnedge.exception.EmailAlreadyTakenException;
import pl.learnedge.exception.UserAlreadyExistException;
import pl.learnedge.exception.UserNotFoundException;
import pl.learnedge.model.PasswordResetToken;
import pl.learnedge.model.User;
import pl.learnedge.repository.PasswordResetTokenRepository;
import pl.learnedge.repository.UserRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordResetTokenRepository resetTokens;
    private final EmailService emailService;

    @Transactional
    public User register(String username, String email, String rawPassword) {
        if (users.existsByUsername(username)) {
            throw new UserAlreadyExistException();
        }
        if (email != null && !email.isBlank() && users.existsByEmail(email)) {
            throw new EmailAlreadyTakenException();
        }

        var user = User.builder()
                .username(username)
                .email(email)
                .password(encoder.encode(rawPassword))
                .role("ROLE_USER")
                .enabled(true)
                .build();

        return users.save(user);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        var user = users.findByEmail(email)
                .orElse(null); // We don't inform if the email exists for security reasons

        if (user != null) {
            // Invalidate previous tokens
            resetTokens.findByUserEmailOrderByCreatedAtDesc(email)
                    .ifPresent(token -> {
                        token.setUsed(true);
                        resetTokens.save(token);
                    });

            // Generate new token
            var token = UUID.randomUUID().toString();
            var resetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                    .used(false)
                    .build();

            resetTokens.save(resetToken);
            emailService.sendPasswordResetEmail(email, token);
        }
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        return resetTokens.findByToken(token)
                .filter(resetToken -> !resetToken.isExpired() && !resetToken.isUsed())
                .map(resetToken -> {
                    User user = resetToken.getUser();
                    user.setPassword(encoder.encode(newPassword));
                    resetToken.setUsed(true);
                    users.save(user);
                    resetTokens.save(resetToken);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public User updateProfile(Long userId, UpdateProfileDto profileDto) {
        User user = users.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Użytkownik nie został znaleziony."));

        user.setFirstName(profileDto.getFirstName());
        user.setLastName(profileDto.getLastName());

        return users.save(user);
    }

    public int getLearningStyleByUserId(Long id) {
        return users.findById(id)
                .map(User::getLearningStyle)
                .map(style -> switch (style.toUpperCase()) {
                    case "AUDITORY" -> 1;
                    case "VISUAL" -> 2;
                    case "KINESTHETIC" -> 3;
                    default -> 0;
                })
                .orElse(0);
    }

    public List<User> getAllUsers() {
        return users.findAll();
    }

    public User getUserById(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User adminUpdateUser(Long id, User updated) {
        User u = getUserById(id);

        u.setFirstName(updated.getFirstName());
        u.setLastName(updated.getLastName());
        u.setEmail(updated.getEmail());
        u.setRole(updated.getRole());
        u.setLearningStyle(updated.getLearningStyle());

        return users.save(u);
    }

    @Transactional
    public void deleteUser(Long id) {
        users.deleteById(id);
    }


}