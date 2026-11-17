package edu.sjsu.cmpe172.tutoring.ai;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import edu.sjsu.cmpe172.tutoring.service.MetricsService;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:sqlite:target/it-agent.db",
    "notification.base-url=http://localhost:1",   // notification service "down"
    "reminder.cron=-"                              // disable the scheduled agent
})
class AgentBookingToolTest {

    @Autowired BookingTools tools;
    @Autowired PendingActionStore pending;
    @Autowired JdbcTemplate jdbc;
    @Autowired MetricsService metrics;

    private static ToolContext as(long userId, String username) {
        return new ToolContext(Map.of("userId", userId, "username", username));
    }

    @BeforeEach
    void reset() {
        jdbc.update("DELETE FROM appointments");
        jdbc.update("UPDATE availability_slots SET version = 0, start_at = datetime('now', '+30 days'), "
                + "end_at = datetime('now', '+30 days', '+30 minutes') WHERE id = 1");
        jdbc.update("INSERT OR IGNORE INTO users(id, username, password_hash, role) VALUES (100, 'agentUser1', 'x', 'CUSTOMER')");
        jdbc.update("INSERT OR IGNORE INTO users(id, username, password_hash, role) VALUES (101, 'agentUser2', 'x', 'CUSTOMER')");
        pending.clear(100L);
        pending.clear(101L);
    }

    private int bookedCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM appointments WHERE slot_id = 1 AND status = 'BOOKED'", Integer.class);
    }

    @Test
    void agentCannotBookOnTheFirstTurn() {
        pending.nextTurn(100L);                                 // user message 1
        tools.bookSlot(1L, as(100, "agentUser1"));              // only prepare
        String result = tools.confirmAction(as(100, "agentUser1"));   // same turn

        assertTrue(result.startsWith("REFUSED"));
        assertEquals(0, bookedCount());
    }

    @Test
    void agentBooksAfterTheUserConfirmsAndUsesTheTransactionalPath() {
        int before = metrics.snapshot().get("bookingsSucceeded");

        pending.nextTurn(100L);                                 // message 1: "book slot 1"
        tools.bookSlot(1L, as(100, "agentUser1"));
        pending.nextTurn(100L);                                 // message 2: "yes, confirm"
        String result = tools.confirmAction(as(100, "agentUser1"));

        assertTrue(result.startsWith("SUCCESS"));
        assertEquals(1, bookedCount());
        // the metric is incremented only inside BookingFacade, so this proves the facade path was used
        assertEquals(before + 1, metrics.snapshot().get("bookingsSucceeded"));
    }

    @Test
    void doubleBookingIsStillBlockedWhenTheAgentBooks() {
        int failedBefore = metrics.snapshot().get("bookingsFailed");

        // user 1 books slot 1 
        pending.nextTurn(100L);
        tools.bookSlot(1L, as(100, "agentUser1"));
        pending.nextTurn(100L);
        assertTrue(tools.confirmAction(as(100, "agentUser1")).startsWith("SUCCESS"));

        // user 2 tries to book the same slot 
        pending.nextTurn(101L);
        tools.bookSlot(1L, as(101, "agentUser2"));
        pending.nextTurn(101L);
        String result = tools.confirmAction(as(101, "agentUser2"));

        assertTrue(result.startsWith("FAILED"));
        assertEquals(1, bookedCount());                         // still one booking
        assertEquals(100L, jdbc.queryForObject(
            "SELECT customer_id FROM appointments WHERE slot_id = 1 AND status = 'BOOKED'", Long.class));
        assertEquals(failedBefore + 1, metrics.snapshot().get("bookingsFailed"));
    }

    @Test
    void agentCannotCancelSomeoneElsesAppointment() {
        pending.nextTurn(100L);
        tools.bookSlot(1L, as(100, "agentUser1"));
        pending.nextTurn(100L);
        tools.confirmAction(as(100, "agentUser1"));
        Long appointmentId = jdbc.queryForObject("SELECT id FROM appointments WHERE slot_id = 1", Long.class);

        pending.nextTurn(101L);
        String result = tools.cancelAppointment(appointmentId, as(101, "agentUser2"));   // not their appointment

        assertTrue(result.startsWith("You have no appointment"));
        assertEquals(1, bookedCount());
    }
}