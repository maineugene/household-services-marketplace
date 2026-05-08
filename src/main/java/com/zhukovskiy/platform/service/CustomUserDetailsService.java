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
    //private final Map<String, User> users = new HashMap<>();
    //private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        /*User user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }*/

        return userRepository.findByEmail(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found" + username));

        /*return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles("USER")
                .build();*/
    }

    /*public void registerUser(String username, String password) throws Exception {
        if (users.containsKey(username)) {
            throw new Exception("User already exists");
        } else {
            String encodedPassword = passwordEncoder.encode(password);
            users.put(username, new User(username, encodedPassword));
        }
    }*/

    //TODO через этот метод нужно сохранять пользователя в репозиторий чтобы хранить все данные

    public void registerUser(RegistrationForm form) {
        User user = User.builder()
                .email(form.getEmail())
                .firstName(form.getFirstName())
                .lastName(form.getLastName())
                .country(form.getCountry())
                .dob(form.getDob())
                .password(passwordEncoder.encode(form.getPassword()))
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        /*user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));

        user.setFirstName(form.getFirstName());
        user.setCountry(form.getCountry());
        user.setDob(form.getDob());*/
    }
}
