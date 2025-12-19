package com.isaqcasey.aidocsassistant.Impl;

import com.isaqcasey.aidocsassistant.DTO.LoginResponse;
import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Repo.UserRepo;
import com.isaqcasey.aidocsassistant.Service.JWTService;
import com.isaqcasey.aidocsassistant.Service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserImpl implements UserService
{
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
    public User store(User user)
    {
        try
        {
            if(repo.findUserByUserName(user.getUserName()) == null && repo.findUserByEmail(user.getEmail()) == null)
            {
                user.setUserName(user.getUserName());
                user.setEmail(user.getEmail());
                user.setPassword(encoder.encode(user.getPassword()));

                repo.save(user);

                return user;
            }

            return null;
        }
        catch(Exception error)
        {
            System.out.println("Error occured: " + error.getMessage());

            return null;
        }
    }

    // USER LOGIN
    public LoginResponse login(User user)
    {
        User userFound = repo.findUserByUserName(user.getUserName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(user.getPassword(), userFound.getPassword()))
        {
            throw new RuntimeException("Invalid password");
        }

        String token = jwt.generateToken(userFound.getUserName());

        return new LoginResponse(token);
    }
}