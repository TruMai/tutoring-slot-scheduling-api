package edu.sjsu.cmpe172.tutoring;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import edu.sjsu.cmpe172.tutoring.service.AppointmentService;

@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:sqlite:target/it-concurrency.db")
public class ConcurrencyTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetSlot() {
        jdbcTemplate.update("DELETE FROM appointments");
        // make slot 1 free, in the future, and back at version 0
        jdbcTemplate.update("UPDATE availability_slots SET version = 0, "
            + "start_at = datetime('now', '+30 days'), "
            + "end_at = datetime('now', '+30 days', '+30 minutes') WHERE id = 1");
        // customers 100 and 101 must exist in case foreign keys are enforced
        jdbcTemplate.update("INSERT OR IGNORE INTO users(id, username, password_hash, role) "
            + "VALUES (100, 'concUser1', 'x', 'CUSTOMER')");
        jdbcTemplate.update("INSERT OR IGNORE INTO users(id, username, password_hash, role) "
            + "VALUES (101, 'concUser2', 'x', 'CUSTOMER')");
    }

    @Test
    public void testConcurrentBookingFailsForSecondUser() throws InterruptedException {
        Long slotId = 1L;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (long customerId : new long[] {100L, 101L}) {
            executor.submit(() -> {
                try {
                    latch.await();
                    appointmentService.bookSlot(customerId, slotId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("Booking by " + customerId + " failed: " + e);
                    failureCount.incrementAndGet();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());
    }
}