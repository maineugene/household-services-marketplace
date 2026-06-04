package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.RegistrationForm;
import com.zhukovskiy.platform.model.Role;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomUserDetailsService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@test.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .country("Russia")
                .password("encoded_pass")
                .role(Role.CUSTOMER)
                .dob(LocalDate.of(1990, 1, 1))
                .build();
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@test.com");

        assertThat(details.getUsername()).isEqualTo("test@test.com");
        assertThat(details.getPassword()).isEqualTo("encoded_pass");
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void loadUserByUsername_shouldThrowWhenNotFound() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void registerUser_withRole_shouldEncodePasswordAndSave() {
        RegistrationForm form = RegistrationForm.builder()
                .email("new@test.com")
                .firstName("Petr")
                .lastName("Petrov")
                .country("Russia")
                .dob(LocalDate.of(1990, 1, 1))
                .password("rawpass")
                .build();

        when(passwordEncoder.encode("rawpass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registerUser(form, Role.SPECIALIST);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@test.com");
        assertThat(saved.getPassword()).isEqualTo("encoded");
        assertThat(saved.getRole()).isEqualTo(Role.SPECIALIST);
        assertThat(saved.getFirstName()).isEqualTo("Petr");
    }

    @Test
    void registerUser_defaultRole_shouldBeCustomer() {
        RegistrationForm form = RegistrationForm.builder()
                .email("new@test.com")
                .firstName("Petr")
                .lastName("Petrov")
                .country("Russia")
                .dob(LocalDate.of(1990, 1, 1))
                .password("rawpass")
                .build();

        when(passwordEncoder.encode("rawpass")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registerUser(form);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }
}
