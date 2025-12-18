package com.isaqcasey.aidocsassistant.Repo;

import com.isaqcasey.aidocsassistant.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepo extends JpaRepository<User, Long>
{
    User findUserByUserName(String userName);
    User findUserByEmail(String email);
}
