package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.model.Role;
import com.zhukovskiy.platform.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void getAuthorities_shouldReturnRolePrefixed() {
        User user = User.builder()
                .id(1L).email("test@test.com").firstName("Ivan").lastName("Ivanov")
                .country("Russia").password("pass").role(Role.SPECIALIST)
                .dob(LocalDate.of(1990, 1, 1)).build();

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_SPECIALIST");
    }

    @Test
    void getPassword_shouldReturnUserPassword() {
        User user = User.builder()
                .id(1L).email("test@test.com").firstName("Ivan").lastName("Ivanov")
                .country("Russia").password("secret123").role(Role.CUSTOMER)
                .dob(LocalDate.of(1990, 1, 1)).build();

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getPassword()).isEqualTo("secret123");
    }

    @Test
    void getUsername_shouldReturnEmail() {
        User user = User.builder()
                .id(1L).email("user@example.com").firstName("Ivan").lastName("Ivanov")
                .country("Russia").password("pass").role(Role.CUSTOMER)
                .dob(LocalDate.of(1990, 1, 1)).build();

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getUsername()).isEqualTo("user@example.com");
    }

    @Test
    void getUser_shouldReturnWrappedUser() {
        User user = User.builder()
                .id(1L).email("test@test.com").firstName("Ivan").lastName("Ivanov")
                .country("Russia").password("pass").role(Role.ADMIN)
                .dob(LocalDate.of(1990, 1, 1)).build();

        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.getUser()).isEqualTo(user);
        assertThat(details.getUser().getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void authorities_shouldVaryByRole() {
        User moderator = User.builder()
                .id(1L).email("mod@test.com").firstName("Mod").lastName("User")
                .country("Russia").password("pass").role(Role.MODERATOR)
                .dob(LocalDate.of(1990, 1, 1)).build();

        CustomUserDetails details = new CustomUserDetails(moderator);

        assertThat(details.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_MODERATOR");
    }
}
