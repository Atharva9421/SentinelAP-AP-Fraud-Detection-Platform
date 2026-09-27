package com.sentinelap.web;

import com.sentinelap.domain.AppUser;
import com.sentinelap.dto.AuthDtos.LoginRequest;
import com.sentinelap.dto.AuthDtos.LoginResponse;
import com.sentinelap.repo.AppUserRepository;
import com.sentinelap.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail(), Map.of(
                "org", user.getOrganization(),
                "role", user.getRole()
        ));

        return new LoginResponse(token, user.getEmail(), user.getFullName(), user.getOrganization(), user.getRole());
    }
}
