package edu.sjsu.cmpe172.tutoring.dto;
import java.time.LocalDateTime;
public record SlotResponse(Long id, Long providerId, Long serviceId, LocalDateTime startTime, LocalDateTime endTime, int version) {
    
}
