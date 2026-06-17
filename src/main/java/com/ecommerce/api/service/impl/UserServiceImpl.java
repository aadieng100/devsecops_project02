package com.ecommerce.api.service.impl;

import com.ecommerce.api.exception.BadRequestException;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.model.User;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Override
    @Transactional
    public User createUser(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException(
                    "A user with username '" + username + "' already exists.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException(
                    "A user with email '" + email + "' already exists.");
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .build();
        return userRepository.save(user);
    }
}
