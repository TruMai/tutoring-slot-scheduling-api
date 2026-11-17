package edu.sjsu.cmpe172.tutoring.dto;

public record AppointmentSummary(Long id, Long slotId, String customerName, String providerName,
                                 String serviceName, String startAt, String endAt, String status) {}
