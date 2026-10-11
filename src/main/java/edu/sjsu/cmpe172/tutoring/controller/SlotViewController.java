package edu.sjsu.cmpe172.tutoring.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SlotViewController {

    @GetMapping("/slots")
    public String viewSlots(Model model) {
        // fake data slot browsing page renders
        List<MockSlot> mockSlots = List.of(
            new MockSlot(1L, "Computer Science 172", "Monday, 10:00 AM - 11:30 AM"),
            new MockSlot(2L, "Data Structures & Algorithms", "Tuesday, 2:00 PM - 3:30 PM"),
            new MockSlot(3L, "Computer Architecture", "Wednesday, 11:30 AM - 1:00 PM")
        );
        
        model.addAttribute("slots", mockSlots);
        
        return "slots"; // templates/slots.html
    }

    public static record  MockSlot(Long id, String subject, String time) {}
}