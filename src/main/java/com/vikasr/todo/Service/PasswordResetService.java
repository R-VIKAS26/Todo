package com.vikasr.todo.Service;

import com.vikasr.todo.DTO.ForgotPasswordRequestDTO;
import com.vikasr.todo.DTO.ResetPasswordRequestDTO;

public interface PasswordResetService {
    
    void forgotPassword(ForgotPasswordRequestDTO request);
    
    void resetPassword(ResetPasswordRequestDTO request);
    
    boolean validateResetToken(String token);
}
