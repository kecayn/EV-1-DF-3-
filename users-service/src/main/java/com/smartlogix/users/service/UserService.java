package com.smartlogix.users.service;

import com.smartlogix.users.domain.User;
import com.smartlogix.users.dto.CreateUserRequest;
import com.smartlogix.users.dto.CredentialCheckRequest;
import com.smartlogix.users.dto.CredentialCheckResponse;
import com.smartlogix.users.dto.UpdateUserRequest;
import com.smartlogix.users.dto.UserResponse;
import com.smartlogix.users.exception.UserAlreadyExistsException;
import com.smartlogix.users.exception.UserNotFoundException;
import com.smartlogix.users.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already in use: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already in use: " + request.getEmail());
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        User saved = userRepository.save(user);
        log.info("Created user id={} username={}", saved.getId(), saved.getUsername());
        return toResponse(saved);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            if (!request.getUsername().equals(user.getUsername())
                    && userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already in use: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equals(user.getEmail())
                    && userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already in use: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        User saved = userRepository.save(user);
        log.info("Updated user id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
        log.info("Deleted user id={}", id);
    }

    public CredentialCheckResponse checkCredentials(CredentialCheckRequest request) {
        return userRepository.findByUsername(request.getUsername())
                .filter(u -> passwordEncoder.matches(request.getRawPassword(), u.getPasswordHash()))
                .map(u -> new CredentialCheckResponse(true, u.getId(), u.getUsername()))
                .orElse(new CredentialCheckResponse(false, null, null));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
}
