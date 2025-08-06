package com.chatalyst.backend.dto;

import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "JWT authentication response")
public class JwtResponse {
    
    @Schema(description = "JWT access token")
    private String token;
    
    @Schema(description = "Token type", example = "Bearer")
    private String type = "Bearer";
    
    @Schema(description = "User ID")
    private Long id;
    
    @Schema(description = "User email")
    private String email;
    
    @Schema(description = "User first name")
    private String firstName;
    
    @Schema(description = "User last name")
    private String lastName;
    
    @Schema(description = "User roles")
    private java.util.List<String> roles;
    
    public JwtResponse(String accessToken, Long id, String email, String firstName, String lastName, java.util.List<String> roles) {
        this.token = accessToken;
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
    }
}
