package edu.sjsu.cmpe172.tutoring.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import edu.sjsu.cmpe172.tutoring.dto.NotificationRequest;

@Component
public class NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificationGateway.class);

    private static final int TIMEOUT_MS = 2000;   // timeout
    private static final int MAX_ATTEMPTS = 3;    // retry

    private final RestTemplate restTemplate;
    private final String url;

    public NotificationGateway(@Value("${notification.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
        this.url = baseUrl + "/api/notifications";
    }

    // Never throws. A notification problem must not break a booking.
    public void send(NotificationRequest request) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                restTemplate.postForEntity(url, request, String.class);
                log.info("Notification sent type={} key={} attempt={}",
                        request.type(), request.idempotencyKey(), attempt);
                return;   // success, stop retrying
            } catch (Exception e) {
                log.warn("Notification attempt {}/{} failed key={}: {}",
                        attempt, MAX_ATTEMPTS, request.idempotencyKey(), e.getMessage());
            }
        }
        // fallback: give up, log it, and let the booking stand
        log.error("Notification failed after {} attempts, continuing without it. key={}",
                MAX_ATTEMPTS, request.idempotencyKey());
    }
}