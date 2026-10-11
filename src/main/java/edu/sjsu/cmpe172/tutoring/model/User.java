package edu.sjsu.cmpe172.tutoring.model;

public class User{
    private Long id;
    private String username;
    private String passwordHash;
    private String role;

    public User(Long id, String username, String passwordHash, String role){
        this.id =id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId(){
        return id;
    }
    public String getUsername(){
        return username;
    }
    public String getPasswordHash(){
        return passwordHash;
    }
    public String getRole(){
        return role;
    }

}