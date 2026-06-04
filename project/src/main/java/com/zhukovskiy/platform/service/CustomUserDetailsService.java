package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.dto.RegistrationForm;
import com.zhukovskiy.platform.model.Role;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
         return userRepository.findByEmail(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found" + username));
    }

    //TODO через этот метод нужно сохранять пользователя в репозиторий чтобы хранить все данные

    public void registerUser(RegistrationForm form, Role role) {
        User user = User.builder()
                .email(form.getEmail())
                .firstName(form.getFirstName())
                .lastName(form.getLastName())
                .country(form.getCountry())
                .dob(form.getDob())
                .password(passwordEncoder.encode(form.getPassword()))
                .role(role) // Теперь можно выбрать роль при регистрации
                .build();

        userRepository.save(user);
    }

    public void registerUser(RegistrationForm form) {
        registerUser(form, Role.CUSTOMER);
    }
}
