package edu.sjsu.cmpe172.tutoring.repository;

import edu.sjsu.cmpe172.tutoring.model.Appointment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class AppointmentRepository {

    private final JdbcTemplate jdbcTemplate;

    // for database access
    public AppointmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Inserts a new appointment record into database and returns created Appointment entity
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
}