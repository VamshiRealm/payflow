package com.payflow.payflow.controller;

import com.payflow.payflow.dto.AuthResponse;
import com.payflow.payflow.dto.RegisterRequest;
import com.payflow.payflow.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.login(body.get("email"), body.get("password")));
    }
}