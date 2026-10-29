package edu.sjsu.cmpe172.tutoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import edu.sjsu.cmpe172.tutoring.model.Appointment;
import edu.sjsu.cmpe172.tutoring.service.BookingFacade;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:target/it-test.db",
    "notification.base-url=http://localhost:1"   
})
class BookingWhenNotificationDownTest {

    @Autowired
    BookingFacade bookingFacade;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearAppointments() {
        jdbcTemplate.update("DELETE FROM appointments");
    }

    @Test
    void bookingStillSucceedsWhenNotificationServiceIsDown() {
        Appointment appointment = bookingFacade.book(1L, "sjsustudentA", 1L);

        assertEquals("BOOKED", appointment.status());

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM appointments WHERE id = ? AND status = 'BOOKED'",
            Integer.class, appointment.id());
        assertEquals(1, count);
    }
}