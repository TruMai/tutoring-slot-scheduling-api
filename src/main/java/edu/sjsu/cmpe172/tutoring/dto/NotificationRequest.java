package edu.sjsu.cmpe172.tutoring.dto;

public record NotificationRequest(String recipient, String type, String message, String idempotencyKey) {}
