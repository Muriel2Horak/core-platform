package com.example.api.controller;

import com.example.api.dto.UserDto;
import com.example.api.service.KeycloakAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final KeycloakAdminService keycloakAdminService;

    public UserController(KeycloakAdminService keycloakAdminService) {
        this.keycloakAdminService = keycloakAdminService;
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<UserDto> getProfileByUsername(@PathVariable String username, Authentication authentication) {
        try {
            // 🔍 DEBUG: Kdo je přihlášený vs. koho hledáme
            String loggedInUser = authentication.getName();
            log.info("🔍 AUTH_DEBUG: Logged in user: {}, requested profile: {}", loggedInUser, username);
            log.info("🔍 AUTH_DEBUG: Authentication details: {}", authentication.getDetails());
            log.info("🔍 AUTH_DEBUG: Authentication principal: {}", authentication.getPrincipal());

            log.info("Getting profile for username: {}", username);
            UserDto user = keycloakAdminService.getUserByUsername(username);

            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error while getting profile for username: {}", username, e);
            return ResponseEntity.status(500).build();
        }
    }
}