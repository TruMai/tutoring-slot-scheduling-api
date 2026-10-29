package edu.sjsu.cmpe172.tutoring.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import edu.sjsu.cmpe172.tutoring.model.Appointment;

@Repository
public class AppointmentRepository {

    private final JdbcTemplate jdbcTemplate;

    // for database access
    public AppointmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // inserts new appointment record into database and returns Appointment entity
    public Appointment createAppointment(Long customerId, Long slotId) {
        String sql = "INSERT INTO appointments (customer_id, slot_id, status) VALUES (?, ?, 'BOOKED')";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // Execute insert and retrieve generated primary key ID
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
}