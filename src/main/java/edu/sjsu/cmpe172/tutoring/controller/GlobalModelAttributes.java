package edu.sjsu.cmpe172.tutoring.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

// adds "username" to the model of every page, so the navbar can show Login or Logout
@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("username")
    public String username(HttpServletRequest request) {
        HttpSession session = request.getSession(false);   // don't create a session just to look
        return session == null ? null : (String) session.getAttribute("USERNAME");
    }
}