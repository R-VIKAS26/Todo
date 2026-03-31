package com.vikasr.todo.Service.impl;

import com.vikasr.todo.DTO.AuthenticationRequestDTO;
import com.vikasr.todo.DTO.AuthenticationResponseDTO;
import com.vikasr.todo.DTO.RegisterRequestDTO;
import com.vikasr.todo.config.JwtTokenUtil;
import com.vikasr.todo.Model.User;
import com.vikasr.todo.Repository.UserRepo;
import com.vikasr.todo.Service.AuthenticationService;
import com.vikasr.todo.exception.EmailAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private AuthenticationManager authenticationManager;
    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Autowired
    public void setUserRepo(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setJwtTokenUtil(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public AuthenticationResponseDTO authenticate(AuthenticationRequestDTO request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepo.findByEmail(request.getEmail())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtTokenUtil.generateToken(user);
        
        return AuthenticationResponseDTO.builder()
                .token(token)
                .email(user.getEmail())
                .build();
    }

    @Override
    public AuthenticationResponseDTO register(RegisterRequestDTO request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(User.Role.USER);

        userRepo.save(user);

        String token = jwtTokenUtil.generateToken(user);
        
        return AuthenticationResponseDTO.builder()
                .token(token)
                .email(user.getEmail())
                .build();
    }
}
