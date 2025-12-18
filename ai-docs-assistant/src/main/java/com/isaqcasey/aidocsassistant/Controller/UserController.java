package com.isaqcasey.aidocsassistant.Controller;

import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class UserController
{
    private final UserService service;

    // DEPENDENCY INJECTION
    public UserController(UserService service)
    {
        this.service = service;
    }

    // DISPLAY SIGNUP PAGE/INFO
    // Yea so nilagay ko lang to para may signup endpoint info pag ginet mo yung /user/signup
    @GetMapping("/user/signup")
    public Map<String, Object> signupPage()
    {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User Signup API");
        response.put("method", "POST");
        response.put("endpoint", "/user/signup");
        response.put("required_fields", Map.of(
            "user_name", "Your username (unique)",
            "email", "Your email address (unique)",
            "password", "Your password (will be encrypted)"
        ));
        response.put("example", Map.of(
            "user_name", "john_doe",
            "email", "john@example.com",
            "password", "securepassword123"
        ));
        return response;
    }

    // USE FOR USER REGISTRATION
    @PostMapping("/user/signup")
    public User store(@RequestBody User user)
    {
        return service.store(user);
    }
}
