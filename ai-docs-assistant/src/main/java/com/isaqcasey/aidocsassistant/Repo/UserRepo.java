package com.isaqcasey.aidocsassistant.Repo;

import com.isaqcasey.aidocsassistant.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Long>
{
    Optional<User> findUserByUserName(String userName);
    Optional<User> findUserByEmail(String email);
}
