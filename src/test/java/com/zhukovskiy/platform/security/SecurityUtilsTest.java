package com.zhukovskiy.platform.security;

import com.zhukovskiy.platform.model.Role;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityUtils securityUtils;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_shouldReturnAuthenticatedUser() {
        User user = User.builder()
                .id(1L).email("test@test.com").firstName("Ivan").lastName("Ivanov")
                .country("Russia").password("pass").role(Role.CUSTOMER)
                .dob(LocalDate.of(1990, 1, 1)).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@test.com", null, Collections.emptyList()));

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        User result = securityUtils.getCurrentUser();

        assertThat(result).isEqualTo(user);
        assertThat(result.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    void getCurrentUser_shouldThrowWhenUserNotFound() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("missing@test.com", null, Collections.emptyList()));

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> securityUtils.getCurrentUser())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найден");
    }
}
