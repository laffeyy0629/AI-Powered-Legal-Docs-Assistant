package com.isaqcasey.aidocsassistant.Service;

import com.isaqcasey.aidocsassistant.Model.User;
import org.springframework.validation.BindingResult;

import java.util.Map;

public interface UserService
{
    Map<String, Object> store(User user);
    Map<String, Object> login(User user);
    Map<String, Object> getInputValidationResult(BindingResult bindingResult);

    public interface OnCreate {} // For registration
    public interface OnLogin {}  // For login
}
