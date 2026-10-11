package edu.sjsu.cmpe172.tutoring;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.sjsu.cmpe172.tutoring.service.AppointmentService;

@SpringBootTest
public class ConcurrencyTest {

    @Autowired
    private AppointmentService appointmentService;

    @Test
    public void testConcurrentBookingFailsForSecondUser() throws InterruptedException {
        Long slotId = 1L;
        Long customer1Id = 100L;
        Long customer2Id = 101L;
        
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Task for User 1
        Runnable task1 = () -> {
            try {
                latch.await(); // Wait for trigger signal
                appointmentService.bookSlot(customer1Id, slotId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        // Task for User 2
        Runnable task2 = () -> {
            try {
                latch.await(); // Wait for trigger signal
                appointmentService.bookSlot(customer2Id, slotId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        latch.countDown(); // Release both threads simultaneously

        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.sleep(40);
        }

        // 1 booking must succeed and 1 fail with 409 conflict
        assertEquals(1, successCount.get());
        assertEquals(1, failureCount.get());
    }
}