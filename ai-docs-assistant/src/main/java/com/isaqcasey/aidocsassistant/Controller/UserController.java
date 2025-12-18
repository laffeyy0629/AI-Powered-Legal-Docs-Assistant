package com.isaqcasey.aidocsassistant.Controller;

import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Service.UserService;
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
}
