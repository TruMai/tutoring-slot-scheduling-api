package edu.sjsu.cmpe172.tutoring.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.sjsu.cmpe172.tutoring.service.MetricsService;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;
    private final MetricsService metrics;

    public HealthController(JdbcTemplate jdbcTemplate, MetricsService metrics) {
        this.jdbcTemplate = jdbcTemplate;
        this.metrics = metrics;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);   // database check
            return ResponseEntity.ok(Map.of("status", "UP", "database", "UP"));
        } catch (Exception e) {
            log.error("Health check: database unreachable: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("status", "DOWN", "database", "DOWN"));
        }
    }

    @GetMapping("/metrics")
    public Map<String, Integer> metrics() {
        return metrics.snapshot();
    }
}