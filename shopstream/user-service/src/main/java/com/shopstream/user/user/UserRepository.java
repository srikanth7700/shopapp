package com.shopstream.user.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data generates the SQL from the method names at startup.
 * findByEmailIgnoreCase -> SELECT ... WHERE UPPER(email) = UPPER(?)
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
