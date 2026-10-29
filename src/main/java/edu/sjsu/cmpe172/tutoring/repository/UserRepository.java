package edu.sjsu.cmpe172.tutoring.repository;
import edu.sjsu.cmpe172.tutoring.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public User findByUsername(String username){
        String sql = "SELECT id, username, password_hash, role FROM users WHERE username = ?";
        List<User> users = jdbcTemplate.query(sql, (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("role")
        ), username);

        if (users.isEmpty()){
            return null;
        }
        return users.get(0);
    }

    public User saveUser(String username, String passwordHash, String role){
        String sql = "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, role);
            return ps;
        }, keyHolder);

        Long newId = keyHolder.getKey().longValue();
        return new User(newId, username, passwordHash, role);
    }

    public void createProviderProfile(Long userId, String displayName){
        String sql = "INSERT INTO providers (user_id, display_name) VALUES (?, ?)";
        jdbcTemplate.update(sql, userId, displayName);
    }
}
