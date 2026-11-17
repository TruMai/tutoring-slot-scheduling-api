package edu.sjsu.cmpe172.tutoring.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import edu.sjsu.cmpe172.tutoring.dto.AppointmentSummary;
import edu.sjsu.cmpe172.tutoring.model.Appointment;

@Repository
public class AppointmentRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // joins appointment + customer + slot + provider + service for list views
    private static final String SELECT_SUMMARY = """
        SELECT a.id, a.slot_id, a.status,
               u.username AS customerName,
               p.display_name AS providerName,
               srv.name AS serviceName,
               s.start_at, s.end_at
        FROM appointments a
        JOIN users u ON a.customer_id = u.id
        JOIN availability_slots s ON a.slot_id = s.id
        JOIN providers p ON s.provider_id = p.id
        JOIN services srv ON s.service_id = srv.id
        """;

    private final RowMapper<AppointmentSummary> summaryMapper = (rs, n) -> new AppointmentSummary(
        rs.getLong("id"),
        rs.getLong("slot_id"),
        rs.getString("customerName"),
        rs.getString("providerName"),
        rs.getString("serviceName"),
        rs.getString("start_at"),
        rs.getString("end_at"),
        rs.getString("status"));

    private final JdbcTemplate jdbcTemplate;

    // for database access
    public AppointmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // inserts new appointment record in database and returns Appointment entity
    public Appointment createAppointment(Long customerId, Long slotId) {
        String sql = "INSERT INTO appointments (customer_id, slot_id, status) VALUES (?, ?, 'BOOKED')";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // execute insert and retrieve generated primary key ID
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, customerId);
            ps.setLong(2, slotId);
            return ps;
        }, keyHolder);

        Long newId = keyHolder.getKey().longValue();
        return new Appointment(newId, customerId, slotId, "BOOKED");
    }

    public int cancelAppointment(Long customerId, Long appointmentId){
        String sql = "UPDATE appointments SET status = 'CANCELLED' WHERE id = ? AND customer_id = ? and status = 'BOOKED'";
        return jdbcTemplate.update(sql, appointmentId, customerId);
    }

    public Appointment findByIdAndCustomer(Long appointmentId, Long customerId) {
        String sql = "SELECT id, customer_id, slot_id, status FROM appointments WHERE id = ? AND customer_id = ?";
        List<Appointment> r = jdbcTemplate.query(sql, (rs, n) -> new Appointment(
            rs.getLong("id"), rs.getLong("customer_id"), rs.getLong("slot_id"), rs.getString("status")),
            appointmentId, customerId);
        return r.isEmpty() ? null : r.get(0);
    }

    /** BOOKED appointments whose slot has ended is COMPLETED. Returns rows changed */
    public int completeFinished() {
        String sql = """
            UPDATE appointments SET status = 'COMPLETED'
            WHERE status = 'BOOKED'
              AND slot_id IN (SELECT id FROM availability_slots WHERE end_at <= ?)
            """;
        return jdbcTemplate.update(sql, LocalDateTime.now().format(FORMATTER));
    }

    /** upcoming = BOOKED; history = CANCELLED or COMPLETED. */
    public List<AppointmentSummary> findForCustomer(Long customerId, boolean upcoming) {
        String filter = upcoming
            ? "AND a.status = 'BOOKED' ORDER BY s.start_at"
            : "AND a.status IN ('CANCELLED', 'COMPLETED') ORDER BY s.start_at DESC";
        return jdbcTemplate.query(SELECT_SUMMARY + " WHERE a.customer_id = ? " + filter, summaryMapper, customerId);
    }

    public List<AppointmentSummary> findForProvider(Long providerId) {
        return jdbcTemplate.query(SELECT_SUMMARY + " WHERE s.provider_id = ? ORDER BY s.start_at DESC",
            summaryMapper, providerId);
    }
    
    public List<AppointmentSummary> findDueForReminder(LocalDateTime from, LocalDateTime to) {
    String sql = SELECT_SUMMARY + " WHERE a.status = 'BOOKED' AND a.reminder_sent = 0 "
               + "AND s.start_at > ? AND s.start_at <= ? ORDER BY s.start_at";
    return jdbcTemplate.query(sql, summaryMapper, from.format(FORMATTER), to.format(FORMATTER));
    }

    public int markReminderSent(Long appointmentId) {
        return jdbcTemplate.update("UPDATE appointments SET reminder_sent = 1 WHERE id = ?", appointmentId);
    }
}