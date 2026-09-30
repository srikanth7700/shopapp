package com.shopstream.user.config;

import com.shopstream.user.user.Role;
import com.shopstream.user.user.User;
import com.shopstream.user.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the admin account on startup if it does not exist yet.
 * Done in code (not a SQL seed) because the password must be BCrypt-hashed.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                   AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmailIgnoreCase(adminProperties.email())) {
            return;
        }
        User admin = new User(adminProperties.email().toLowerCase(),
                passwordEncoder.encode(adminProperties.password()),
                adminProperties.fullName(),
                Role.ADMIN);
        userRepository.save(admin);
        log.info("Created admin account {}", adminProperties.email());
    }
}
