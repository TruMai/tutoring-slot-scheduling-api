package edu.sjsu.cmpe172.tutoring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import edu.sjsu.cmpe172.tutoring.service.AppointmentService;
import jakarta.servlet.http.HttpSession;

// provider view of appointments booked with them
@RestController
@RequestMapping("/api/provider")
public class ProviderAppointmentController {

    private final AppointmentService appointmentService;

    public ProviderAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/appointments")
    public ResponseEntity<?> myAppointments(HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        String role = (String) session.getAttribute("ROLE");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Must be logged in");
        }
        if (!"PROVIDER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only PROVIDER users can view their appointments");
        }
        try {
            return ResponseEntity.ok(appointmentService.getProviderAppointments(userId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}