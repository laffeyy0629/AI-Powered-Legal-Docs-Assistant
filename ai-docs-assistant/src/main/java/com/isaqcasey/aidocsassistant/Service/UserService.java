package com.isaqcasey.aidocsassistant.Service;

import com.isaqcasey.aidocsassistant.Model.User;
import org.springframework.validation.BindingResult;

import java.util.Map;

public interface UserService
{
    Map<String, Object> store(User user);
    Map<String, Object> login(User user);
    Map<String, Object> getInputValidationResult(BindingResult bindingResult);
    Map<String, Object> verifyEmail(String token);
    Map<String, Object> resendVerificationEmail(String email);
    Map<String, Object> forgotPassword(String email);
    Map<String, Object> resetPassword(String token, String newPassword);
    Map<String, Object> forgotUsername(String email);
    Map<String, Object> getUserProfile(String username);
    Map<String, Object> getOAuthLinkInfo(String email, String sessionId);
    Map<String, Object> confirmOAuthLink(String email, String provider, String sessionId);
    Map<String, Object> unlinkOAuth(String username, String password, String provider);

    public interface OnCreate {} // For registration
    public interface OnLogin {}  // For login
}
