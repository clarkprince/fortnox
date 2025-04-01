package com.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dto.LoginRequest;
import com.dto.LoginResponse;
import com.entities.Tenant;
import com.entities.User;
import com.repository.UserRepository;
import com.services.TenantService;
import com.utils.JwtUtil;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TenantService tenantService;

    private BCryptPasswordEncoder passwordEncoder;

    public UserController() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable int id) {
        return userRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        // Validate required fields
        if (user.getEmail() == null || user.getEmail().trim().isEmpty() || user.getFirstName() == null || user.getFirstName().trim().isEmpty()
                || user.getLastName() == null || user.getLastName().trim().isEmpty() || user.getPassword() == null
                || user.getPassword().trim().isEmpty() || user.getTenant() == null || user.getTenant().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("All fields (email, firstName, lastName, password, tenant) are required");
        }

        // Convert and validate tenant
        String tenantDomain = user.getTenant().toLowerCase().trim();
        Tenant tenant = tenantService.getTenantByDomain(tenantDomain); // this will throw an exception if tenant not found
        user.setTenant(tenant.getSynchroteamDomain());

        // Check if email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }

        // Set default role if not specified
        if (user.getRole() == null || user.getRole().trim().isEmpty()) {
            user.setRole("1");
        }

        user.setActive(true);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable int id, @RequestBody User user) {
        return userRepository.findById(id).map(existingUser -> {
            user.setId(id);
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable int id) {
        return userRepository.findById(id).map(user -> {
            userRepository.deleteById(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        return userRepository.findByEmail(loginRequest.getEmail())
                .filter(user -> passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).map(user -> {
                    String token = jwtUtil.generateToken(user);
                    LoginResponse response = new LoginResponse(token, user.getFullName(), user.getEmail(), user.getRole(), user.getTenant());
                    return ResponseEntity.ok(response);
                }).orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
