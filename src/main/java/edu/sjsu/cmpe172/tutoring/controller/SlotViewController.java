package edu.sjsu.cmpe172.tutoring.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import edu.sjsu.cmpe172.tutoring.service.SlotService;

@Controller
public class SlotViewController {
    private final SlotService slotService;
    private static final int PAGE_SIZE = 20;

    public SlotViewController(SlotService slotService){
        this.slotService = slotService;
    }

    @GetMapping("/slots")
    public String viewSlots(@RequestParam(required = false) Long providerId,
                            @RequestParam(required = false) Long serviceId,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        int p = Math.max(page, 0);
        model.addAttribute("slots", slotService.getAvailableSlots(providerId, serviceId, PAGE_SIZE, p * PAGE_SIZE));
        model.addAttribute("page", p);
        model.addAttribute("providerId", providerId);
        model.addAttribute("serviceId", serviceId);
        return "slots"; // templates/slots.html
    }
}