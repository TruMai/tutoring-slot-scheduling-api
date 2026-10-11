package edu.sjsu.cmpe172.tutoring.dto;

public record RegisterRequest(String username, String password, String role, String displayName){}