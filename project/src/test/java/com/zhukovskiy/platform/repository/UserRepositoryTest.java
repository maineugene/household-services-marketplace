package com.zhukovskiy.platform.repository;

import com.zhukovskiy.platform.model.Role;
import com.zhukovskiy.platform.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void shouldSaveAndFindUser(){
        User user = User.builder()
                .email("test@mail.com")
                .password("raw_password")
                .role(Role.CUSTOMER)
                .firstName("Ivan")
                .lastName("Ivanov")
                .country("Russia")
                .dob(LocalDate.now())
                .build();

        User saved = userRepository.save(user);
        assertThat(saved.getId()).isNotNull();
    }
}
