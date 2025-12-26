package com.isaqcasey.aidocsassistant.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@aidocs.com}")
    private String fromEmail;

    @Value("${spring.mail.from.name:AI Docs Assistant}")
    private String fromName;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send verification email to user
     * @param toEmail Recipient email address
     * @param username User's username
     * @param token Verification token
     * @return true if email sent successfully
     */
    public boolean sendVerificationEmail(String toEmail, String username, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email - AI Legal Docs Assistant");

            String verificationLink = frontendUrl + "/verify-email?token=" + token;

            String htmlContent = buildVerificationEmailHtml(username, verificationLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
            return true;
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            return false;
        }
    }

    /**
     * Build HTML content for verification email
     */
    private String buildVerificationEmailHtml(String username, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border: 1px solid #ddd;
                        border-radius: 5px;
                        padding: 30px;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .header h1 {
                        color: #4F46E5;
                        margin: 0;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 30px;
                        background-color: #4F46E5;
                        color: white !important;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 20px 0;
                        font-weight: bold;
                    }
                    .button:hover {
                        background-color: #4338CA;
                    }
                    .footer {
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #ddd;
                        font-size: 12px;
                        color: #666;
                    }
                    .warning {
                        background-color: #FEF3C7;
                        border-left: 4px solid #F59E0B;
                        padding: 10px;
                        margin: 20px 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>AI Legal Docs Assistant</h1>
                    </div>
                    
                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>Thank you for registering with AI Legal Docs Assistant! To complete your registration and activate your account, please verify your email address.</p>
                    
                    <div style="text-align: center;">
                        <a href="%s" class="button">Verify Email Address</a>
                    </div>
                    
                    <p>Or copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; color: #4F46E5;">%s</p>
                    
                    <div class="warning">
                        <p><strong>⚠️ Important:</strong></p>
                        <ul>
                            <li>This verification link will expire in <strong>24 hours</strong></li>
                            <li>If you didn't create an account, please ignore this email</li>
                        </ul>
                    </div>
                    
                    <div class="footer">
                        <p>This is an automated message, please do not reply to this email.</p>
                        <p>&copy; 2025 AI Legal Docs Assistant. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, verificationLink, verificationLink);
    }

    /**
     * Send password reset email (for future implementation)
     */
    public boolean sendPasswordResetEmail(String toEmail, String username, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Password - AI Legal Docs Assistant");

            String resetLink = frontendUrl + "/reset-password?token=" + token;

            String htmlContent = buildPasswordResetEmailHtml(username, resetLink);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
            return true;
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            return false;
        }
    }

    /**
     * Build HTML content for password reset email
     */
    private String buildPasswordResetEmailHtml(String username, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .container {
                        background-color: #f9f9f9;
                        border: 1px solid #ddd;
                        border-radius: 5px;
                        padding: 30px;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .header h1 {
                        color: #4F46E5;
                        margin: 0;
                    }
                    .button {
                        display: inline-block;
                        padding: 12px 30px;
                        background-color: #EF4444;
                        color: white !important;
                        text-decoration: none;
                        border-radius: 5px;
                        margin: 20px 0;
                        font-weight: bold;
                    }
                    .button:hover {
                        background-color: #DC2626;
                    }
                    .footer {
                        margin-top: 30px;
                        padding-top: 20px;
                        border-top: 1px solid #ddd;
                        font-size: 12px;
                        color: #666;
                    }
                    .warning {
                        background-color: #FEE2E2;
                        border-left: 4px solid #EF4444;
                        padding: 10px;
                        margin: 20px 0;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>AI Legal Docs Assistant</h1>
                    </div>
                    
                    <p>Hello <strong>%s</strong>,</p>
                    
                    <p>We received a request to reset your password. Click the button below to create a new password:</p>
                    
                    <div style="text-align: center;">
                        <a href="%s" class="button">Reset Password</a>
                    </div>
                    
                    <p>Or copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; color: #4F46E5;">%s</p>
                    
                    <div class="warning">
                        <p><strong>⚠️ Important:</strong></p>
                        <ul>
                            <li>This reset link will expire in <strong>1 hour</strong></li>
                            <li>If you didn't request a password reset, please ignore this email</li>
                            <li>Your password will remain unchanged until you create a new one</li>
                        </ul>
                    </div>
                    
                    <div class="footer">
                        <p>This is an automated message, please do not reply to this email.</p>
                        <p>&copy; 2025 AI Legal Docs Assistant. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, resetLink, resetLink);
    }

    /**
     * Send username reminder email
     */
    public boolean sendUsernameReminderEmail(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Username Reminder - AI Legal Docs Assistant");

            String htmlContent = buildUsernameReminderEmailHtml(username);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Username reminder email sent to: {}", toEmail);
            return true;
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send username reminder email to: {}", toEmail, e);
            return false;
        }
    }

    /**
     * Build HTML content for username reminder email
     */
    private String buildUsernameReminderEmailHtml(String username) {
        String loginLink = frontendUrl + "/login";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                    }
                    .header h1 {
                        color: #4F46E5;
                        margin: 0;
                    }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .username-box { background: white; border: 2px solid #4F46E5; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center; }
                    .username { font-size: 24px; font-weight: bold; color: #4F46E5; word-break: break-all; }
                    .button { display: inline-block; padding: 12px 30px; background: #4F46E5; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    .info { background: #EFF6FF; border-left: 4px solid #3B82F6; padding: 15px; margin: 20px 0; }
                  
                    
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Username Reminder</h1>
                    </div>
                    <div class="content">
                        <p>Hello,</p>
                        <p>You requested a reminder of your username for <strong>AI Legal Docs Assistant</strong>.</p>
                        
                        <div class="username-box">
                            <p style="margin: 0; color: #666; font-size: 14px;">Your Username:</p>
                            <div class="username">%s</div>
                        </div>
                        
                        <div class="info">
                            <p><strong>💡 Quick Tip:</strong></p>
                            <p>Save your username in a safe place or use a password manager to avoid forgetting it in the future.</p>
                        </div>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">Go to Login</a>
                        </div>
                        
                        <p style="margin-top: 30px; color: #666; font-size: 14px;">If you didn't request this reminder, you can safely ignore this email. Your account remains secure.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply to this email.</p>
                        <p>&copy; 2025 AI Legal Docs Assistant. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, loginLink);
    }

    /**
     * Send OAuth account linking notification email
     * Industry standard: Notify users when OAuth is linked to their account
     */
    public boolean sendOAuthLinkedEmail(String email, String username, String provider) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(provider + " Account Linked - AI Legal Docs Assistant");
            helper.setText(buildOAuthLinkedEmailContent(username, provider), true);

            mailSender.send(message);
            log.info("OAuth linked notification email sent to: {}", email);
            return true;
        } catch (Exception e) {
            log.error("Failed to send OAuth linked email to {}: {}", email, e.getMessage());
            return false;
        }
    }

    /**
     * Build OAuth linked notification email content
     */
    private String buildOAuthLinkedEmailContent(String username, String provider) {
        String dashboardLink = frontendUrl + "/dashboard";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9fafb; padding: 30px; border-radius: 0 0 10px 10px; }
                    .button { display: inline-block; padding: 12px 30px; background: #4F46E5; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .footer { text-align: center; margin-top: 30px; color: #666; font-size: 12px; }
                    .alert { background: #FEF3C7; border-left: 4px solid #F59E0B; padding: 15px; margin: 20px 0; }
                    .provider-box { background: white; border: 2px solid #4F46E5; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔗 OAuth Account Linked</h1>
                    </div>
                    <div class="content">
                        <p>Hello <strong>%s</strong>,</p>
                        <p>Your <strong>%s</strong> account has been successfully linked to your AI Legal Docs Assistant account.</p>
                        
                        <div class="provider-box">
                            <p style="margin: 0; color: #666; font-size: 14px;">Linked OAuth Provider:</p>
                            <h2 style="margin: 10px 0; color: #4F46E5;">%s</h2>
                        </div>
                        
                        <p><strong>You can now sign in using:</strong></p>
                        <ul style="line-height: 2;">
                            <li>✅ Your username and password (original method)</li>
                            <li>✅ %s Sign-In (newly linked)</li>
                        </ul>
                        
                        <div class="alert">
                            <p style="margin: 0;"><strong>⚠️ Security Notice:</strong></p>
                            <p style="margin: 5px 0 0 0;">If you didn't perform this action, please secure your account immediately by changing your password and reviewing your account activity.</p>
                        </div>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">Go to Dashboard</a>
                        </div>
                        
                        <p style="margin-top: 30px; color: #666; font-size: 14px;">This notification was sent because an OAuth provider was linked to your account. If you have any concerns, please contact our support team.</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply to this email.</p>
                        <p>&copy; 2025 AI Legal Docs Assistant. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, provider, provider, provider, dashboardLink);
    }
}

