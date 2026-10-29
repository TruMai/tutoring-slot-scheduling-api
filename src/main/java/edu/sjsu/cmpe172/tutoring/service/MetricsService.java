package edu.sjsu.cmpe172.tutoring.service;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

@Component
public class MetricsService {

    // AtomicInteger is an int that is safe when several requests update it at once
    private final AtomicInteger bookingsSucceeded = new AtomicInteger();
    private final AtomicInteger bookingsFailed = new AtomicInteger();

    public void bookingSucceeded() { bookingsSucceeded.incrementAndGet(); }
    public void bookingFailed()    { bookingsFailed.incrementAndGet(); }

    public Map<String, Integer> snapshot() {
        return Map.of(
            "bookingsSucceeded", bookingsSucceeded.get(),
            "bookingsFailed", bookingsFailed.get());
    }
}