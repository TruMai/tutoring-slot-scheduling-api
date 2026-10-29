package edu.sjsu.cmpe172.tutoring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.sjsu.cmpe172.tutoring.dto.BookSlotRequest;
import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import edu.sjsu.cmpe172.tutoring.exception.SlotAlreadyBookedException;
import edu.sjsu.cmpe172.tutoring.model.Appointment;
import edu.sjsu.cmpe172.tutoring.service.AppointmentService;
import jakarta.servlet.http.HttpSession;

// manages bookings and cancellations
@RestController
@RequestMapping("/api/customer")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }
    
    @PostMapping("/book")
    public ResponseEntity<?> bookSlot(@RequestBody BookSlotRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        String role = (String) session.getAttribute("ROLE");

        // Guard Check
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Must be logged in");
        }
        // Gurad Check
        if (!"CUSTOMER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only CUSTOMER users can book slots");
        }
        try {
            Appointment appointment = appointmentService.bookSlot(userId, request.slotId());
            return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SlotAlreadyBookedException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @DeleteMapping("/appointments/{appointmentId}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long appointmentId, HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        String role = (String) session.getAttribute("ROLE");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Must be logged in");
        }

        if (!"CUSTOMER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only CUSTOMER users can cancel appointments");
        }
        try {
            appointmentService.cancelAppointment(userId, appointmentId);
            return ResponseEntity.ok("Appointment cancelled successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}