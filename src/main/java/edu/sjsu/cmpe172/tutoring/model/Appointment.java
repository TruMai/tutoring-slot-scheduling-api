package edu.sjsu.cmpe172.tutoring.model;

public record Appointment(Long id, Long customerId, Long slotId, String status) {}
