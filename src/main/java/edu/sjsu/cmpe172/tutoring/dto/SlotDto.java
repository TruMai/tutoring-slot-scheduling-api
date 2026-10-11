package edu.sjsu.cmpe172.tutoring.dto;

public record SlotDto(Long id, String providerName, String serviceName, Double price, Integer durationMin, String startAt, String endAt, Integer version) {}
