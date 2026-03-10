package com.example.bankcards.controller;

import com.example.bankcards.dto.JwtResponse;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.validators.AuthDetailsValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthDetailsValidator authDetailsValidator;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        log.info("User login attempt: {}", request.getUsername());

        Errors errors = new BeanPropertyBindingResult(request, "request");
        authDetailsValidator.validate(request, errors);
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(errors.getAllErrors());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtService.generateToken((UserDetails) authentication.getPrincipal());

        log.info("User {} has been successfully logged in.", request.getUsername());
        return ResponseEntity.ok(new JwtResponse(jwt));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        log.info("Starting registration of a new user: {}", request.getUsername());

        Errors errors = new BeanPropertyBindingResult(request, "request");
        authDetailsValidator.validate(request, errors);
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(errors.getAllErrors());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = request
                .getRoles()
                .stream()
                .map(roleRepository::findByName)
                .map(role -> role.orElseThrow(RuntimeException::new))
                .collect(Collectors.toSet());

        user.setRoles(roles);

        userRepository.save(user);

        log.info("User {} successfully created with roles: {}", user.getUsername(), request.getRoles());
        return ResponseEntity.ok("Пользователь успешно зарегистрировался!");
    }

}