package edu.sjsu.cmpe172.tutoring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ConfirmationController {

    @GetMapping("/confirmation")
    public String showConfirmation(@RequestParam Long appointmentId, Model model) {
        // Pass a mock appointment object matching confirmation.html
        MockAppointment appointment = new MockAppointment(appointmentId);
        model.addAttribute("appointment", appointment);
        return "confirmation";
    }

    public record MockAppointment(Long id) {}
}