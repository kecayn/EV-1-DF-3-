package com.smartlogix.users.service;

import com.smartlogix.users.domain.User;
import com.smartlogix.users.dto.CreateUserRequest;
import com.smartlogix.users.dto.CredentialCheckRequest;
import com.smartlogix.users.dto.CredentialCheckResponse;
import com.smartlogix.users.dto.UserResponse;
import com.smartlogix.users.exception.UserAlreadyExistsException;
import com.smartlogix.users.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void createUser_hashesPassword() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("secret123");

        UserResponse response = userService.createUser(req);

        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void createUser_throwsWhenUsernameExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("secret123");

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void checkCredentials_returnsValidForCorrectPassword() {
        String raw = "secret123";
        String hash = passwordEncoder.encode(raw);

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash(hash);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        CredentialCheckRequest req = new CredentialCheckRequest();
        req.setUsername("alice");
        req.setRawPassword(raw);

        CredentialCheckResponse response = userService.checkCredentials(req);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void checkCredentials_returnsInvalidForWrongPassword() {
        String hash = passwordEncoder.encode("secret123");

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash(hash);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        CredentialCheckRequest req = new CredentialCheckRequest();
        req.setUsername("alice");
        req.setRawPassword("wrongpassword");

        CredentialCheckResponse response = userService.checkCredentials(req);

        assertThat(response.isValid()).isFalse();
    }
}
