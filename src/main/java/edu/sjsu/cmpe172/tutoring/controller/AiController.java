package edu.sjsu.cmpe172.tutoring.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.sjsu.cmpe172.tutoring.ai.BookingAgent;
import edu.sjsu.cmpe172.tutoring.ai.PolicyQaService;
import edu.sjsu.cmpe172.tutoring.ai.ReminderAgent;
import edu.sjsu.cmpe172.tutoring.dto.AskRequest;
import edu.sjsu.cmpe172.tutoring.dto.ChatRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final BookingAgent bookingAgent;
    private final PolicyQaService policyQaService;
    private final ReminderAgent reminderAgent;

    public AiController(BookingAgent bookingAgent, PolicyQaService policyQaService, ReminderAgent reminderAgent) {
        this.bookingAgent = bookingAgent;
        this.policyQaService = policyQaService;
        this.reminderAgent = reminderAgent;
    }

    // Agent 1: agent acts as logged-in customer
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        String role = (String) session.getAttribute("ROLE");
        String username = (String) session.getAttribute("USERNAME");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Must be logged in");
        }
        if (!"CUSTOMER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only CUSTOMER users can use the booking assistant");
        }
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body("message is required");
        }
        try {
            return ResponseEntity.ok(Map.of("reply", bookingAgent.chat(userId, username, request.message())));
        } catch (Exception e) {
            log.error("Chat agent failed: {}", e.getMessage(), e); //----
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("The AI service is unavailable. Try again later.");
        }
    }

    // policy and FAQ questions, open to everyone
    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            return ResponseEntity.badRequest().body("question is required");
        }
        try {
            return ResponseEntity.ok(Map.of("answer", policyQaService.ask(request.question())));
        } catch (Exception e) {
            log.error("Policy Q&A failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("The AI service is unavailable. Try again later.");
        }
    }

    // Agent 2: trigger for the demo (providers only)
    @PostMapping("/reminders/run")
    public ResponseEntity<?> runReminders(HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        String role = (String) session.getAttribute("ROLE");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Must be logged in");
        }
        if (!"PROVIDER".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only PROVIDER users can run the reminder agent");
        }
        return ResponseEntity.ok(Map.of("remindersSent", reminderAgent.runOnce()));
    }
}