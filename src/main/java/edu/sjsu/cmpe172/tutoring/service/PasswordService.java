package edu.sjsu.cmpe172.tutoring.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final PasswordEncoder passwordEncoder;

    public PasswordService(PasswordEncoder passwordEncoder){
        this.passwordEncoder = passwordEncoder;
    }

    public String hashPassword(String plainText){
        return passwordEncoder.encode(plainText);
    }

    public boolean checkPassword(String plainText, String hash){
        return passwordEncoder.matches(plainText, hash);
    }
}
