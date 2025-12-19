package com.isaqcasey.aidocsassistant.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

// Pwede mo to delete or galawin, nilagyan ko lang to para may default home route

@RestController
public class HomeController
{
    @GetMapping("/")
    public Map<String, Object> home()
    {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to AI-Powered Legal Docs Assistant API");
        response.put("status", "Running");
        response.put("endpoints", Map.of(
            "GET /", "API Information",
            "GET /api/status", "Check API Status",
            "GET /user/signup", "Signup Instructions",
            "POST /user/signup", "Register New User"
        ));
        return response;
    }

    @GetMapping("/api/status")
    public Map<String, String> status()
    {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "API is running successfully!");
        return response;
    }
}

