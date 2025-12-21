package com.isaqcasey.aidocsassistant.Controller;

import com.isaqcasey.aidocsassistant.DTO.LoginResponse;
import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Service.JWTService;
import com.isaqcasey.aidocsassistant.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
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
    public Map<String, Object> store(@Valid @RequestBody User user,  BindingResult bindingResult)
    {
        Map<String, Object> reponse = new HashMap<>();

        if(bindingResult.hasErrors())
        {
            Map<String, Object> errors = new HashMap<>();

            bindingResult.getFieldErrors().forEach(error -> {
                errors.put(error.getField(), error.getDefaultMessage());
            });

            reponse.put("success", false);
            reponse.put("errors", errors);

            return reponse;
        }

        return service.store(user);
    }

    // VALIDATE USER CREDENTIALS
    @PostMapping("/user/login")
    public Map<String, Object> login(@RequestBody User user)
    {
        return service.login(user);
    }

    // AUTHENTICATED USER PURPOSES
    @GetMapping("/api/secure")
    public String secure()
    {
        return "JWT works";
    }

}
