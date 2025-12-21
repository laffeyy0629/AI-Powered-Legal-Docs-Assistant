package com.isaqcasey.aidocsassistant.Impl;

import com.isaqcasey.aidocsassistant.DTO.LoginResponse;
import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Repo.UserRepo;
import com.isaqcasey.aidocsassistant.Service.JWTService;
import com.isaqcasey.aidocsassistant.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserImpl implements UserService
{
    private static final Logger log = LoggerFactory.getLogger(UserImpl.class);
    private final UserRepo repo;
    private final PasswordEncoder encoder;
    private final JWTService jwt;

    // DEPENDENCY INJECTION
    public UserImpl(UserRepo repo, PasswordEncoder encoder, JWTService jwt)
    {
        this.repo    = repo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    // USER REGISTRATION
    @Override
    public Map<String, Object> store(User user)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            if(!repo.findUserByUserName(user.getUserName()).isPresent() && !repo.findUserByEmail(user.getEmail()).isPresent())
            {
                user.setPassword(encoder.encode(user.getPassword()));

                repo.save(user);

                response.put("success", true);
                response.put("message", "Registered Successfully");

                return response;
            }

            response.put("success", false);
            response.put("message", "User already exists");

            return response;
        }
        catch(Exception error)
        {
            log.error("Error occurred: {}", error.getMessage(), error);

            return null;
        }
    }

    // USER LOGIN
    public Map<String, Object> login(User user)
    {
        Map<String, Object> response = new HashMap<>();

        return response;
    }
}