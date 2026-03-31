package com.vikasr.todo.DTO;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationResponseDTO {
    
    private String token;
    private String type = "Bearer";
    private String email;
}
