package com.isaqcasey.aidocsassistant.Impl;

import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Repo.UserRepo;
import com.isaqcasey.aidocsassistant.Service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserImpl implements UserService
{
    private final UserRepo repo;
    private final PasswordEncoder encoder;

    // DEPENDENCY INJECTION
    public UserImpl(UserRepo repo, PasswordEncoder encoder)
    {
        this.repo    = repo;
        this.encoder = encoder;
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
}
