package edu.sjsu.cmpe172.tutoring.controller;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import edu.sjsu.cmpe172.tutoring.model.Appointment;
import edu.sjsu.cmpe172.tutoring.service.BookingFacade;
import edu.sjsu.cmpe172.tutoring.service.SlotService;
import jakarta.servlet.http.HttpSession;

@Controller
public class BookingController {

    private final BookingFacade bookingFacade;   // was AppointmentService
    private final SlotService slotService;

    public BookingController(BookingFacade bookingFacade, SlotService slotService) {
        this.bookingFacade = bookingFacade;
        this.slotService = slotService;
    }

    private Long currentCustomerId(HttpSession session){
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) return null;
        if (!"CUSTOMER".equals(session.getAttribute("ROLE"))){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only CUSTOMER users can book slots");
        }
        return userId;
    }

    @GetMapping("/slots/{id}/book")
    public String showBookingForm(@PathVariable Long id, HttpSession session, Model model) {
        if(currentCustomerId(session) == null) return "redirect:/login";
        try{
            model.addAttribute("slot", slotService.getSlotById(id));
        } catch (ResourceNotFoundException e){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        return "booking-form";
    }

    // Handle form submission, run service logic/locking, and save to database
    @PostMapping("/book")
    public String handleBookingSubmission(@RequestParam Long slotId, HttpSession session, Model model) {
        Long customerId = currentCustomerId(session);
        if(customerId == null) return "redirect:/login";
        String username = (String) session.getAttribute("USERNAME");   // new: needed for the notification
        try {
           Appointment saved = bookingFacade.book(customerId, username, slotId);   // was appointmentService.bookSlot
           return "redirect:/confirmation?appointmentId=" + saved.id();
        } catch (Exception e) {
            // Catches race conditions SlotAlreadyBookedException
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("slot", slotService.getSlotById(slotId));
            return "booking-form";
        }
    }
}