package com.isaqcasey.aidocsassistant.Service;

import com.isaqcasey.aidocsassistant.DTO.LoginResponse;
import com.isaqcasey.aidocsassistant.Model.User;

import java.util.Map;

public interface UserService
{
    Map<String, Object> store(User user);
    Map<String, Object> login(User user);
}
