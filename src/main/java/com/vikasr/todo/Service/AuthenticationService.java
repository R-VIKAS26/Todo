package com.vikasr.todo.Service;

import com.vikasr.todo.DTO.AuthenticationRequestDTO;
import com.vikasr.todo.DTO.AuthenticationResponseDTO;
import com.vikasr.todo.DTO.RegisterRequestDTO;

public interface AuthenticationService {
    
    AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request);
    
    AuthenticationResponseDTO register(RegisterRequestDTO request);
}
