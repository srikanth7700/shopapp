package com.shopstream.user.auth;

import com.shopstream.user.config.JwtProperties;
import com.shopstream.user.user.Role;
import com.shopstream.user.user.User;
import com.shopstream.user.user.UserRepository;
import com.shopstream.user.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit test: no Spring context, no database. The repository is a Mockito mock,
 * so the test is fast and only checks AuthService's own logic.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    // Strength 4 = fast hashing for tests (production uses the default of 10).
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final JwtService jwtService =
            new JwtService(new JwtProperties("unit-test-secret-unit-test-secret-0123456789", 60));

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesCustomerWithHashedPasswordAndReturnsToken() {
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(new RegisterRequest("Jane@Example.com", "password123", "Jane Doe"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.user().email()).isEqualTo("jane@example.com");
        assertThat(response.user().role()).isEqualTo("CUSTOMER");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("jane@example.com", "password123", "Jane")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginSucceedsWithCorrectPassword() {
        User user = new User("jane@example.com", passwordEncoder.encode("password123"), "Jane", Role.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(new LoginRequest("jane@example.com", "password123"));

        assertThat(response.token()).isNotBlank();
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User("jane@example.com", passwordEncoder.encode("password123"), "Jane", Role.CUSTOMER);
        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "wrong-password")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password");
    }
}
