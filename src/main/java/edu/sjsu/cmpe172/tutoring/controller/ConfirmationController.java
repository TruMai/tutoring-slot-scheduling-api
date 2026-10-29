package edu.sjsu.cmpe172.tutoring.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import edu.sjsu.cmpe172.tutoring.service.AppointmentService;
import edu.sjsu.cmpe172.tutoring.service.SlotService;
import edu.sjsu.cmpe172.tutoring.model.Appointment;

import jakarta.servlet.http.HttpSession;



@Controller
public class ConfirmationController {

    private final AppointmentService appointmentService;
    private final SlotService slotService;

    public ConfirmationController(AppointmentService appointmentService, SlotService slotService){
        this.appointmentService = appointmentService;
        this.slotService = slotService;
    }
    @GetMapping("/confirmation")
    public String showConfirmation(@RequestParam Long appointmentId, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("USER_ID");
        if(userId == null) return "redirect:/login";

        try{
            // for logged in customer, can't view other's appointment
            Appointment appointment = appointmentService.getAppointment(userId, appointmentId);
            model.addAttribute("appointment", appointment);
            model.addAttribute("slot", slotService.getSlotById(appointment.slotId()));
        } catch (ResourceNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return "confirmation";
    }

    public record MockAppointment(Long id) {}
}