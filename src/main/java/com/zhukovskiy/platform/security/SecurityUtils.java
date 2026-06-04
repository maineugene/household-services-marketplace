package com.zhukovskiy.platform.security;

import com.zhukovskiy.platform.exception.ResourceNotFoundException;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь", email));
    }
}
