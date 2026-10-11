package edu.sjsu.cmpe172.tutoring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.sjsu.cmpe172.tutoring.model.Appointment;
import edu.sjsu.cmpe172.tutoring.service.AppointmentService;

@Controller
public class BookingController {

    private final AppointmentService appointmentService;

    public BookingController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/slots/{id}/book")
    public String showBookingForm(@PathVariable Long id, Model model) {
        // Pass slot object so ${slot.id} and ${slot.subject} work in Thymeleaf
        SlotView slot = new SlotView(id, "CS Tutoring Session");
        model.addAttribute("slot", slot);
        return "booking-form";
    }

    // 2. Handle form submission, run service logic/locking, and save to database
    @PostMapping("/book")
    public String handleBookingSubmission(@RequestParam Long slotId, Model model) {
        try {
            //  mock customer ID of 1L for UI testing
            Long customerId = 1L;

            // Calls service layer (handles versioning, locking, and JdbcTemplate insert)
            Appointment savedAppointment = appointmentService.bookSlot(customerId, slotId);

            // Redirects to confirmation page
            return "redirect:/confirmation?appointmentId=" + savedAppointment.id();
            
        } catch (Exception e) {
            // Catches race conditions SlotAlreadyBookedException
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("slotId", slotId);
            return "booking-form";
        }
    }
    public record SlotView(Long id, String subject){}
}