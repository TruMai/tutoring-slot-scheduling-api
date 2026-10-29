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

import edu.sjsu.cmpe172.tutoring.dto.SlotDto;
import edu.sjsu.cmpe172.tutoring.dto.SlotResponse;

@Repository
public class SlotRepository {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String SELECT_SLOT = """
        SELECT
            s.id,
            p.display_name AS providerName,
            srv.name AS serviceName,
            srv.price,
            srv.duration_min AS durationMin,
            s.start_at AS startAt,
            s.end_at AS endAt,
            s.version
        FROM availability_slots s
        JOIN providers p ON s.provider_id = p.id
        JOIN services srv ON s.service_id = srv.id
    """;

    private final RowMapper<SlotDto> slotMapper = (rs, rowNum) -> new SlotDto(
        rs.getLong("id"),
        rs.getString("providerName"),
        rs.getString("serviceName"),
        rs.getDouble("price"),
        rs.getInt("durationMin"),
        parseDateTime(rs.getString("startAt")).toString(),
        parseDateTime(rs.getString("endAt")).toString(),
        rs.getInt("version")
    );

    private final JdbcTemplate jdbcTemplate;

    public SlotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Unbooked slots that have not started yet. */
    public List<SlotDto> findAvailableSlots(Long providerId, Long serviceId, int limit, int offset) {
        String sql = SELECT_SLOT + """
            WHERE s.id NOT IN (SELECT slot_id FROM appointments WHERE status = 'BOOKED')
            AND s.start_at > ?
            AND (? IS NULL OR s.provider_id = ?)
            AND (? IS NULL OR s.service_id = ?)
            ORDER BY s.start_at
            LIMIT ? OFFSET ?
        """;
        String now = LocalDateTime.now().format(FORMATTER);
        return jdbcTemplate.query(sql, slotMapper,
            now, providerId, providerId, serviceId, serviceId, limit, offset);
    }

    public List<SlotDto> findProviderSlots(Long providerId) {
        return jdbcTemplate.query(SELECT_SLOT + " WHERE s.provider_id = ?", slotMapper, providerId);
    }

    public SlotDto findSlotById(Long slotId) {
        List<SlotDto> r = jdbcTemplate.query(SELECT_SLOT + " WHERE s.id = ?", slotMapper, slotId);
        return r.isEmpty() ? null : r.get(0);
    }

    /** Start time of a slot, or null if the slot doesn't exist. */
    public LocalDateTime getSlotStartAt(Long slotId) {
        List<String> r = jdbcTemplate.query(
            "SELECT start_at FROM availability_slots WHERE id = ?",
            (rs, n) -> rs.getString("start_at"), slotId);
        return r.isEmpty() ? null : parseDateTime(r.get(0));
    }

    public Integer getSlotVersion(Long slotId){
        String sql = "SELECT version FROM availability_slots WHERE id = ?";
        List<Integer> versions = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getInt("version"), slotId);
        return versions.isEmpty() ? null : versions.get(0);
    }

    public int updateSlotVersion(Long slotId, int currentVersion){
        String sql = "UPDATE availability_slots SET version = version + 1 WHERE id = ? AND version = ?";
        return jdbcTemplate.update(sql, slotId, currentVersion);
    }

    public Long findProviderIdByUserId(Long userId){
        String sql = "SELECT id FROM providers WHERE user_id = ?";
        List<Long> results = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("id"), userId);
        return results.isEmpty() ? null : results.get(0);
    }

    public SlotResponse createSlot(Long providerId, Long serviceId, LocalDateTime startTime, LocalDateTime endTime){
        String sql = "INSERT INTO availability_slots (provider_id, service_id, start_at, end_at, version) VALUES (?, ?, ?, ?, 0)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, providerId);
            if (serviceId != null) {
                ps.setLong(2, serviceId);
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            ps.setString(3, startTime.format(FORMATTER));
            ps.setString(4, endTime.format(FORMATTER));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        Long newId = (key != null) ? key.longValue() : null;
        return new SlotResponse(newId, providerId, serviceId, startTime, endTime, 0);
    }

    public boolean existsOverlappingSlot(Long providerId, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
            SELECT COUNT(*) FROM availability_slots
            WHERE provider_id = ? AND start_at < ? AND end_at > ?
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
            providerId, endTime.format(FORMATTER), startTime.format(FORMATTER));
        return count != null && count > 0;
    }

    // tolerate legacy formats: (yyyy-MM-dd HH:mm:ss), (MM-dd-yyyy), (MM/dd/yyyy HH:mm:ss) ISO
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null) return null;
        try {
            if (dateStr.contains("/")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
            } else if (dateStr.length() == 10) {
                return java.time.LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MM-dd-yyyy")).atStartOfDay();
            }
            return LocalDateTime.parse(dateStr, FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
