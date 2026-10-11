package edu.sjsu.cmpe172.tutoring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.sjsu.cmpe172.tutoring.dto.LoginRequest;
import edu.sjsu.cmpe172.tutoring.dto.RegisterRequest;
import edu.sjsu.cmpe172.tutoring.dto.UserResponse;
import edu.sjsu.cmpe172.tutoring.model.User;
import edu.sjsu.cmpe172.tutoring.service.UserService;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class SecurityController {

    private final UserService userService;

    public SecurityController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(
                    request.username(), 
                    request.password(), 
                    request.role(), 
                    request.displayName()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new UserResponse(user.getId(), user.getUsername(), user.getRole()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        User user = userService.authenticate(request.username(), request.password());
        
        if (user != null) {
            // Save user details into HTTP session
            session.setAttribute("USER_ID", user.getId());
            session.setAttribute("USERNAME", user.getUsername());
            session.setAttribute("ROLE", user.getRole());

            return ResponseEntity.ok(new UserResponse(user.getId(), user.getUsername(), user.getRole()));
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate(); // Destroy session
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        
        String username = (String) session.getAttribute("USERNAME");
        String role = (String) session.getAttribute("ROLE");
        
        return ResponseEntity.ok(new UserResponse(userId, username, role));
    }
}