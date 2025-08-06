package com.chatalyst.backend.security.services;

import com.chatalyst.backend.Entity.Role;
import com.chatalyst.backend.Entity.RoleName;
import com.chatalyst.backend.Entity.User;
import com.chatalyst.backend.Repository.RoleRepository;
import com.chatalyst.backend.Repository.UserRepository;
import com.chatalyst.backend.dto.JwtResponse;
import com.chatalyst.backend.dto.LoginRequest;
import com.chatalyst.backend.dto.RegisterRequest;
import com.chatalyst.backend.security.jwt.JwtUtils;
import com.chatalyst.backend.model.PasswordResetToken;
import com.chatalyst.backend.Repository.PasswordResetTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    @Transactional
    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return new JwtResponse(jwt, userPrincipal.getId(), userPrincipal.getEmail(),
                userPrincipal.getFirstName(), userPrincipal.getLastName(), roles);
    }

    @Transactional
    public void registerUser(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Error: Email is already taken!");
        }

        // Create new user
        User user = new User(registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getFirstName(),
                registerRequest.getLastName());

        // Set default role
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);

        user.setRoles(roles);
        userRepository.save(user);

        log.info("User registered successfully: {}", registerRequest.getEmail());
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Handles the password reset request.
     * Generates a 6-digit code and sends it via email to the user.
     * @param email The email of the user requesting a password reset.
     * @throws RuntimeException if the user is not found.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Error: User with this email not found!"));

        // Удаляем все существующие токены сброса пароля для этого пользователя
        passwordResetTokenRepository.deleteByUser(user);

        // Создаем новый токен (6-значный код)
        PasswordResetToken token = new PasswordResetToken(user);
        passwordResetTokenRepository.save(token);

        // Отправляем email с кодом
        emailService.sendPasswordResetCode(user, token.getToken());

        log.info("Password reset code generated and email sent for user: {}", email);
    }

    /**
     * Resets the user's password using a valid 6-digit code.
     * @param code The 6-digit password reset code.
     * @param newPassword The new password for the user.
     * @throws RuntimeException if the code is invalid or expired, or user not found.
     */
    @Transactional
    public void resetPassword(String code, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(code) // Ищем по коду
                .orElseThrow(() -> new RuntimeException("Error: Invalid or expired password reset code!"));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken); // Удаляем просроченный токен
            throw new RuntimeException("Error: Password reset code has expired!");
        }

        User user = resetToken.getUser();
        if (user == null) {
            throw new RuntimeException("Error: User associated with this code not found!");
        }

        // Обновляем пароль пользователя
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Удаляем токен после использования
        passwordResetTokenRepository.delete(resetToken);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }
}
