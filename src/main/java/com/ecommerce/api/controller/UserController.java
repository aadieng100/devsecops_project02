package com.ecommerce.api.controller;

import com.ecommerce.api.controller.dto.UserRequest;
import com.ecommerce.api.model.User;
import com.ecommerce.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/{id}
     * Fetch user identity properties by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * POST /api/users
     * Provision a new user profile.
     */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserRequest request) {
        User created = userService.createUser(request.getUsername(), request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
