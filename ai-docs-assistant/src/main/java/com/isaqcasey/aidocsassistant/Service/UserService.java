package com.isaqcasey.aidocsassistant.Service;

import com.isaqcasey.aidocsassistant.DTO.LoginResponse;
import com.isaqcasey.aidocsassistant.Model.User;

public interface UserService
{
    User store(User user);
    LoginResponse login(User user);
}
