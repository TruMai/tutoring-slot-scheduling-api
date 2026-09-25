/*package edu.sjsu.cmpe172.tutoring;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
// http://localhost:8080/api/test/db
@RestController
public class TestController {
    
    @Autowired 
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/api/test/db")
    public Map<String, Object> testDb(){
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT id, username, role FROM users");
        List<Map<String, Object>> providers = jdbcTemplate.queryForList("SELECT id, user_id, display_name FROM providers");

        return Map.of(
            "users", users,
            "providers", providers
        );
    }
} */
