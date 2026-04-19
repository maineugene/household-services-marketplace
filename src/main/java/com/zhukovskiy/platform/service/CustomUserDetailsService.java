package com.zhukovskiy.platform.service;

import com.zhukovskiy.platform.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final Map<String, User> users = new HashMap<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }

    public void registerUser(String username, String password) throws Exception {
        if (users.containsKey(username)) {
            throw new Exception("User already exists");
        } else {
            String encodedPassword = passwordEncoder.encode(password);
            users.put(username, new User(username, encodedPassword));
        }
    }

    /*public void registerUser(RegistrationForm form){
        User user = new User();
        user.setUsername(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));

        user.setFirstName(form.getFirstName());
        user.setCountry(form.getCountry());
        user.setDob(form.getDob());
        userRepository.save(user);
    }*/
}
