package edu.sjsu.cmpe172.tutoring.dto;

import java.time.LocalDateTime;

    public record CreateSlotRequest(Long serviceId, LocalDateTime startTime, LocalDateTime endTime) {
}
