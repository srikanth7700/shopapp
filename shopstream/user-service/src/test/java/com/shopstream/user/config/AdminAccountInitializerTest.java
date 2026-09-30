package com.shopstream.user.config;

import com.shopstream.user.user.Role;
import com.shopstream.user.user.User;
import com.shopstream.user.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminProperties adminProperties;

    @InjectMocks
    private AdminAccountInitializer initializer;

    @Test
    void createsLowercaseAdminWithEncodedPasswordWhenAccountDoesNotExist() {
        when(adminProperties.email()).thenReturn("Admin@ShopStream.dev");
        when(adminProperties.password()).thenReturn("bootstrap-password");
        when(adminProperties.fullName()).thenReturn("ShopStream Admin");
        when(userRepository.existsByEmailIgnoreCase("Admin@ShopStream.dev")).thenReturn(false);
        when(passwordEncoder.encode("bootstrap-password")).thenReturn("encoded-password");

        initializer.run(null);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getEmail()).isEqualTo("admin@shopstream.dev");
        assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getValue().getFullName()).isEqualTo("ShopStream Admin");
        assertThat(savedUser.getValue().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void doesNotEncodeOrSaveWhenAdminAlreadyExists() {
        when(adminProperties.email()).thenReturn("admin@shopstream.dev");
        when(userRepository.existsByEmailIgnoreCase("admin@shopstream.dev")).thenReturn(true);

        initializer.run(null);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
        verifyNoInteractions(passwordEncoder);
    }
}
