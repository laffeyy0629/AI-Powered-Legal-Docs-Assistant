package com.isaqcasey.aidocsassistant.Impl;

import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Repo.UserRepo;
import com.isaqcasey.aidocsassistant.Service.JWTService;
import com.isaqcasey.aidocsassistant.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

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

    // INPUT VALIDATOR
    @Override
    public Map<String, Object> getInputValidationResult(BindingResult result)
    {
        Map<String, Object> response = new HashMap<>();

        if(result.hasErrors())
        {
            Map<String, Object> errors = new HashMap<>();

            result.getFieldErrors().forEach(error -> {
                errors.put(error.getField(), error.getDefaultMessage());
            });

            response.put("success", false);
            response.put("errors", errors);

            return response;
        }

        response.put("success", true);

        return response;
    }

    // USER REGISTRATION
    @Override
    public Map<String, Object> store(User user)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            if(user.getPassword().trim().length() < 8)
            {
                response.put("success", false);
                response.put("message", "Password must be at least 8 characters long");

                return response;
            }

            if(!repo.findUserByUserName(user.getUserName().trim()).isPresent() && !repo.findUserByEmail(user.getEmail().trim()).isPresent())
            {
                user.setUserName(user.getUserName().trim());
                user.setEmail(user.getEmail().trim());
                user.setPassword(encoder.encode(user.getPassword().trim()));

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
            Map<String, Object> exception = new HashMap<>();

            exception.put("success", false);
            exception.put("message", "An error occurred during registration");

            return exception;
        }
    }

    // USER LOGIN
    public Map<String, Object> login(User user)
    {
        try
        {
            Map<String, Object> response = new HashMap<>();

            User foundUser = repo.findUserByUserName(user.getUserName().trim()).orElse(null);

            if(foundUser == null)
            {
                response.put("success", false);
                response.put("message", "Username not found");

                return response;
            }

            if(!encoder.matches(user.getPassword().trim(), foundUser.getPassword()))
            {
                response.put("success", false);
                response.put("message", "Incorrect password");

                return response;
            }

            String token = jwt.generateToken(foundUser.getUserName());

            response.put("success", true);
            response.put("message", "Successfully logged in");
            response.put("token", token);

            return response;

        }
        catch(Exception error)
        {
            log.error("Error occurred: {}", error.getMessage(), error);
            Map<String, Object> exception = new HashMap<>();

            exception.put("success", false);
            exception.put("message", "An error occurred during login");

            return exception;
        }
    }
}