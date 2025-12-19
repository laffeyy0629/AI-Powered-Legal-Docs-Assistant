package com.isaqcasey.aidocsassistant.Controller;

import com.isaqcasey.aidocsassistant.DTO.LoginResponse;
import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Service.JWTService;
import com.isaqcasey.aidocsassistant.Service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController
{
    private final UserService service;

    // DEPENDENCY INJECTION
    public UserController(UserService service)
    {
        this.service = service;
    }

    // USE FOR USER REGISTRATION
    @PostMapping("/user/signup")
    public User store(@RequestBody User user)
    {
        return service.store(user);
    }

    // VALIDATE USER CREDENTIALS
    @PostMapping("/user/login")
    public LoginResponse login(@RequestBody User user)
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
