package edu.sjsu.cmpe172.tutoring.controller;

import edu.sjsu.cmpe172.tutoring.dto.SlotDto;
import edu.sjsu.cmpe172.tutoring.service.TutoringService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@RestController
public class SlotController {
    private final TutoringService tutoringService;

    //injection
    public SlotController(TutoringService tutoringService){
        this.tutoringService = tutoringService;
    }

    @GetMapping("/")
    public Map<String, String> home(){
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to home");
        return response;
    }

    @GetMapping("/slots")
    public List<SlotDto> getSlots(){
        return tutoringService.getAvailableSlots();
    }
}
