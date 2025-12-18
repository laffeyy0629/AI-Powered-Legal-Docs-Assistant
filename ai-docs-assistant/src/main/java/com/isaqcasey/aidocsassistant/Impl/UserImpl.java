package com.isaqcasey.aidocsassistant.Impl;

import com.isaqcasey.aidocsassistant.Model.User;
import com.isaqcasey.aidocsassistant.Repo.UserRepo;
import com.isaqcasey.aidocsassistant.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserImpl implements UserService
{
    private static final Logger log = LoggerFactory.getLogger(UserImpl.class);
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
                user.setPassword(encoder.encode(user.getPassword()));

                repo.save(user);

                return user;
            }

            return null;
        }
        catch(Exception error)
        {
            log.error("Error occurred: {}", error.getMessage(), error);

            return null;
        }
    }
}
