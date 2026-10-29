package edu.sjsu.cmpe172.tutoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import edu.sjsu.cmpe172.tutoring.dto.NotificationRequest;
import edu.sjsu.cmpe172.tutoring.gateway.NotificationGateway;
import edu.sjsu.cmpe172.tutoring.model.Appointment;
//MetricsService methods are utilized

@Service
public class BookingFacade {

    private static final Logger log = LoggerFactory.getLogger(BookingFacade.class);

    private final AppointmentService appointmentService;
    private final NotificationGateway gateway;
    private final MetricsService metrics;

    public BookingFacade(AppointmentService appointmentService,
                         NotificationGateway gateway,
                         MetricsService metrics) {
        this.appointmentService = appointmentService;
        this.gateway = gateway;
        this.metrics = metrics;
    }

    public Appointment book(Long customerId, String username, Long slotId) {
        Appointment appointment;
        try {
            // 1. save to the database (the transaction commits when this returns)
            appointment = appointmentService.bookSlot(customerId, slotId);
        } catch (RuntimeException e) {
            metrics.bookingFailed();
            log.warn("Booking rejected customerId={} slotId={} reason={}", customerId, slotId, e.getMessage());
            throw e;
        }
        metrics.bookingSucceeded();
        log.info("Booking created appointmentId={} customerId={} slotId={}",
                appointment.id(), customerId, slotId);

        // 2. only now call the notification service
        gateway.send(new NotificationRequest(username, "BOOKING_CONFIRMED",
                "Your booking #" + appointment.id() + " is confirmed.",
                "book-" + appointment.id()));
        return appointment;
    }

    public void cancel(Long customerId, String username, Long appointmentId) {
        appointmentService.cancelAppointment(customerId, appointmentId);   // commits
        log.info("Appointment cancelled appointmentId={} customerId={}", appointmentId, customerId);

        gateway.send(new NotificationRequest(username, "BOOKING_CANCELLED",
                "Your booking #" + appointmentId + " was cancelled.",
                "cancel-" + appointmentId));
    }
}