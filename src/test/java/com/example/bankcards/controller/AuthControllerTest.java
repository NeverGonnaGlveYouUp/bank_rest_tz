package com.example.bankcards.controller;

import com.example.bankcards.config.SecurityConfig;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.RegisterRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtService;
import com.example.bankcards.security.UserDetailsServiceImpl;
import com.example.bankcards.service.UserService;
import com.example.bankcards.validators.AuthDetailsValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тест контроллера AuthController
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuthDetailsValidator authDetailsValidator;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private UserService userService;

    @BeforeEach
    void setUp() {
        // мок authentication manager
        Authentication authentication = mock(Authentication.class);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mock(UserDetails.class));

        // мок JWT сервиса на генерацию токена
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("test-jwt-token");

        // мок репозитория ролей
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_USER");
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(role));

    }

    /**
     * Тест удачного логина с возвратом Jwt токена
     * Доступ к эндпойнту разрешен через импорт SecurityConfig.class
     */
    @Test
    @DisplayName("Тест удачного логина с возвратом Jwt токена")
    void login_SuccessfulLogin_ReturnsJwtToken() throws Exception {
        // Выполнение метода и проверки
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"));
    }

    @DisplayName("Тест логина, неудачный кейс, срабатыванием валидатора")
    @Test
    void login_InvalidCredentials_ReturnsBadRequest() throws Exception {
        // Подготовка данных
        doAnswer(invocation -> {
            Errors errorsArg = invocation.getArgument(1);
            errorsArg.rejectValue("password", "password.invalid.empty", "Пароль обязателен.");
            return null;
        }).when(authDetailsValidator).validate(any(LoginRequest.class), any(Errors.class));

        // Выполнение метода и проверки
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("password"))
                .andExpect(jsonPath("$[0].defaultMessage").value("Пароль обязателен."))
                .andExpect(jsonPath("$[0].code").value("password.invalid.empty"));
    }

    @DisplayName("Регистрация, удачный кейс")
    @Test
    void register_SuccessfulRegistration_ReturnsSuccessMessage() throws Exception {
        // Подготовка данных
        User user = new User();
        user.setId(1L);
        user.setUsername("newuser");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Выполнение метода и проверки
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser\",\"password\":\"password\",\"roles\":[\"ROLE_USER\"]}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Пользователь успешно зарегистрировался!"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация, неудачный кейс, срабатывание валидатора")
    void register_InvalidRegistration_ReturnsBadRequest() throws Exception {
        // Подготовка данных
        doAnswer(invocation -> {
            Errors errorsArg = invocation.getArgument(1);
            errorsArg.rejectValue("username",
                    "username.invalid.short",
                    "Имя пользователя должно быть не менее ...");
            return null;
        }).when(authDetailsValidator).validate(any(RegisterRequest.class), any(Errors.class));

        // Выполнение метода и проверки
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new\",\"password\":\"password\",\"roles\":[\"ROLE_USER\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("username"))
                .andExpect(jsonPath("$[0].defaultMessage")
                        .value("Имя пользователя должно быть не менее ..."))
                .andExpect(jsonPath("$[0].code").value("username.invalid.short"));
    }

}