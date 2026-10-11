package edu.sjsu.cmpe172.tutoring.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.sjsu.cmpe172.tutoring.dto.CreateSlotRequest;
import edu.sjsu.cmpe172.tutoring.dto.SlotDto;
import edu.sjsu.cmpe172.tutoring.dto.SlotResponse;
import edu.sjsu.cmpe172.tutoring.service.SlotService;
import jakarta.servlet.http.HttpSession;

// manages slots by provider
@RestController // marks class as http web controller
@RequestMapping("/api")
public class SlotController {

    private final SlotService slotService;

    //injection
    public SlotController(SlotService slotService){
        this.slotService = slotService;
    }

    @GetMapping("/slots")
    public List<SlotDto> getSlots(
        @RequestParam(required = false) Long providerId,
        @RequestParam(required = false) Long serviceId,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset) {

        return slotService.getAvailableSlots(providerId, serviceId, limit, offset);
    }

    @PostMapping("/provider/slots")
    public ResponseEntity<?> createSlot(@RequestBody CreateSlotRequest request, HttpSession session){
        Long userId = (Long) session.getAttribute("USER_ID");
        String role = (String) session.getAttribute("ROLE");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Must be logged in");
        }

        if (!"PROVIDER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only PROVIDER users can create slots");
        }
        
        SlotResponse slot = slotService.createSlot(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(slot);
    }

    // view 
    @GetMapping("/provider/slots")
    public ResponseEntity<?> getProviderSlots(HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        String role = (String) session.getAttribute("ROLE");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Must be logged in");
        }

        if (!"PROVIDER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only PROVIDER users can view appointments");
        }

        List<SlotDto> slots = slotService.getProviderSlots(userId);
        return ResponseEntity.ok(slots);
    }

}
