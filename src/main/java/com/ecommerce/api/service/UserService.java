package com.ecommerce.api.service;

import com.ecommerce.api.model.User;

public interface UserService {

    User getUserById(Long id);

    User createUser(String username, String email);
}
