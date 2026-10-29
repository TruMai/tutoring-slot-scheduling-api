package edu.sjsu.cmpe172.tutoring.controller;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.sjsu.cmpe172.tutoring.dto.NotificationRequest;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    // keys we have already handled, so a retry doesn't store the same notification twice
    private final Set<String> seenKeys = ConcurrentHashMap.newKeySet();

    // set notification.mock.down=true in application.properties to simulate an outage
    @Value("${notification.mock.down:false}")
    private boolean down;

    @PostMapping
    public ResponseEntity<String> receive(@RequestBody NotificationRequest request) {
        if (down) {
            return ResponseEntity.status(503).body("Service unavailable");
        }
        if (seenKeys.add(request.idempotencyKey())) {
            log.info("Notification stored: type={} recipient={} message={}",
                    request.type(), request.recipient(), request.message());
        } else {
            log.info("Duplicate notification ignored: key={}", request.idempotencyKey());
        }
        return ResponseEntity.ok("SENT");
    }
}