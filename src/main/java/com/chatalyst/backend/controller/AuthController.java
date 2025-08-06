package com.chatalyst.backend.controller;

import com.chatalyst.backend.dto.JwtResponse;
import com.chatalyst.backend.dto.LoginRequest;
import com.chatalyst.backend.dto.MessageResponse;
import com.chatalyst.backend.dto.RegisterRequest;
import com.chatalyst.backend.security.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// DTO для запроса сброса пароля по email
class ForgotPasswordRequest {
    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$", message = "Invalid email format")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

// DTO для запроса установки нового пароля с кодом
class ResetPasswordRequest {
    @NotBlank(message = "Code cannot be empty")
    @Size(min = 6, max = 6, message = "Code must be 6 digits")
    @Pattern(regexp = "^[0-9]{6}$", message = "Code must be 6 digits")
    private String code; // Изменено с 'token' на 'code'

    @NotBlank(message = "New password cannot be empty")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$",
            message = "Password must contain at least: 1 lowercase letter, 1 uppercase letter, 1 digit, and 1 special character (!@#$%^&*()_+-=[]{};':\"\\|,.<>/?)"
    )
    private String newPassword;

    public String getCode() { // Изменено с getToken на getCode
        return code;
    }

    public void setCode(String code) { // Изменено с setToken на setCode
        this.code = code;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication management APIs")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signin")
    @Operation(summary = "Sign in user", description = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse jwtResponse = authService.authenticateUser(loginRequest);
            log.info("User signed in successfully: {}", loginRequest.getEmail());
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Error: Invalid email or password!"));
        }
    }

    @PostMapping("/signup")
    @Operation(summary = "Register new user", description = "Register a new user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email already exists or validation error",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (authService.existsByEmail(registerRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Email is already taken!"));
            }

            authService.registerUser(registerRequest);
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));

        } catch (Exception e) {
            log.error("Registration failed for user: {}", registerRequest.getEmail(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset", description = "Sends a password reset code to the user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset code sent successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "User not found or other error",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest) {
        try {
            authService.requestPasswordReset(forgotPasswordRequest.getEmail());
            log.info("Password reset requested for email: {}", forgotPasswordRequest.getEmail());
            return ResponseEntity.ok(new MessageResponse("Password reset code sent to your email!"));
        } catch (Exception e) {
            log.error("Password reset request failed for email: {}: {}", forgotPasswordRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password", description = "Resets user password using a valid 6-digit code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired code, or other error",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
    })
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
        try {
            authService.resetPassword(resetPasswordRequest.getCode(), resetPasswordRequest.getNewPassword()); // Передаем код
            log.info("Password reset successfully using code");
            return ResponseEntity.ok(new MessageResponse("Password has been reset successfully!"));
        } catch (Exception e) {
            log.error("Password reset failed for code: {}: {}", resetPasswordRequest.getCode(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
