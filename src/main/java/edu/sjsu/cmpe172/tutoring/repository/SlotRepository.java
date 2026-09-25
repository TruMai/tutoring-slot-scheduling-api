package edu.sjsu.cmpe172.tutoring.repository;

import edu.sjsu.cmpe172.tutoring.dto.SlotDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SlotRepository {

    private final JdbcTemplate jdbcTemplate;

    public SlotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SlotDto> findAvailableSlots() {
        String sql = """
            SELECT
                s.id,
                p.display_name AS providerName,
                srv.name AS serviceName,
                srv.price,
                srv.duration_min AS durationMin,
                s.start_at AS startAt,
                s.end_at AS endAt
            FROM availability_slots s
            JOIN providers p ON s.provider_id = p.id
            JOIN services srv ON s.service_id = srv.id
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new SlotDto(
            rs.getLong("id"),
            rs.getString("providerName"),
            rs.getString("serviceName"),
            rs.getDouble("price"),
            rs.getInt("durationMin"),
            rs.getString("startAt"),
            rs.getString("endAt")
        ));
    }
}