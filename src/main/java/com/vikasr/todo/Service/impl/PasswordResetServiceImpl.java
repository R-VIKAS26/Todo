package com.vikasr.todo.Service.impl;

import com.vikasr.todo.DTO.ForgotPasswordRequestDTO;
import com.vikasr.todo.DTO.ResetPasswordRequestDTO;
import com.vikasr.todo.Model.User;
import com.vikasr.todo.Repository.UserRepo;
import com.vikasr.todo.Service.EmailService;
import com.vikasr.todo.Service.PasswordResetService;
import com.vikasr.todo.exception.ResetTokenExpiredException;
import com.vikasr.todo.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private String frontendUrl;
    private int resetTokenExpirationHours;

    @Autowired
    public void setUserRepo(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    @Value("${app.frontend.url:http://localhost:4200}")
    public void setFrontendUrl(@Nullable String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Value("${app.password.reset.expiration.hours:1}")
    public void setResetTokenExpirationHours(@Nullable Integer resetTokenExpirationHours) {
        this.resetTokenExpirationHours = resetTokenExpirationHours;
    }

    @Override
    public void forgotPassword(ForgotPasswordRequestDTO request) {
        User user = userRepo.findByEmail(request.getEmail())
            .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(resetTokenExpirationHours));
        
        userRepo.save(user);

        // Send reset email
        sendPasswordResetEmail(user, resetToken);
    }

    @Override
    public void resetPassword(ResetPasswordRequestDTO request) {
        User user = userRepo.findByPasswordResetToken(request.getToken())
            .orElseThrow(() -> new ResetTokenExpiredException("Invalid reset token"));

        // Check if token is expired
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new ResetTokenExpiredException("Reset token has expired");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        
        userRepo.save(user);
        
        // Send confirmation email
        sendPasswordResetConfirmationEmail(user);
    }

    @Override
    public boolean validateResetToken(String token) {
        return userRepo.findByPasswordResetToken(token)
            .map(user -> !user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now()))
            .orElse(false);
    }

    private void sendPasswordResetEmail(User user, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;
        
        String subject = "Password Reset Request";
        String body = buildPasswordResetEmail(user.getName(), resetLink);
        
        emailService.sendEmail(user.getEmail(), subject, body);
    }

    private void sendPasswordResetConfirmationEmail(User user) {
        String subject = "Password Reset Successful";
        String body = buildPasswordResetConfirmationEmail(user.getName());
        
        emailService.sendEmail(user.getEmail(), subject, body);
    }

    private String buildPasswordResetEmail(String name, String resetLink) {
        return String.format(
            "Hello %s,\n\n" +
            "You requested a password reset. Click the link below to reset your password:\n\n" +
            "%s\n\n" +
            "This link will expire in %d hours.\n\n" +
            "If you didn't request this, please ignore this email.\n\n" +
            "Best regards,\n" +
            "Todo App Team",
            name, resetLink, resetTokenExpirationHours
        );
    }

    private String buildPasswordResetConfirmationEmail(String name) {
        return String.format(
            "Hello %s,\n\n" +
            "Your password has been successfully reset.\n\n" +
            "You can now log in with your new password.\n\n" +
            "Best regards,\n" +
            "Todo App Team",
            name
        );
    }
}
