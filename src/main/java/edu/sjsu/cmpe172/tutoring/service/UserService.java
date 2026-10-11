package edu.sjsu.cmpe172.tutoring.service;

import edu.sjsu.cmpe172.tutoring.model.User;
import edu.sjsu.cmpe172.tutoring.repository.UserRepository; // createProviderProfile, saveUser, findByUsername
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public UserService(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Transactional
    public User registerUser(String username, String plainTextPassword, String role, String displayName) {
        if (!"CUSTOMER".equals(role) && !"PROVIDER".equals(role)) {
            throw new IllegalArgumentException("Role must be CUSTOMER or PROVIDER");
        }

        if (userRepository.findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        String hashedPassword = passwordService.hashPassword(plainTextPassword);
        User user = userRepository.saveUser(username, hashedPassword, role);

        if ("PROVIDER".equals(role)) {
            String name = (displayName != null && !displayName.trim().isEmpty()) ? displayName : username;
            userRepository.createProviderProfile(user.getId(), name);
        }

        return user;
    }

    public User authenticate(String username, String plainTextPassword) {
        User user = userRepository.findByUsername(username);
        
        // If user exists and password matches
        if (user != null && passwordService.checkPassword(plainTextPassword, user.getPasswordHash())) {
            return user;
        }
        
        return null; // Return null if fail
    }
}