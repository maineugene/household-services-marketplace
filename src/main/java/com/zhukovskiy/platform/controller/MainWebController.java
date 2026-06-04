package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.dto.RegistrationForm;
import com.zhukovskiy.platform.model.Role;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.UserRepository;
import com.zhukovskiy.platform.service.CustomUserDetails;
import com.zhukovskiy.platform.service.CustomUserDetailsService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@AllArgsConstructor
@Slf4j
public class MainWebController {

    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/greet")
    public String greet(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {

            try {
                CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
                User user = userDetails.getUser();
                model.addAttribute("isAuthenticated", true);
                model.addAttribute("user", user);
                model.addAttribute("userRole", user.getRole().name());

                // Приветственное сообщение в зависимости от роли
                String welcomeMessage = "";
                if (user.getRole() == Role.CUSTOMER) {
                    welcomeMessage = "Найдите лучших специалистов для ваших задач!";
                } else if (user.getRole() == Role.SPECIALIST) {
                    welcomeMessage = "Находите заказы и зарабатывайте!";
                } else if (user.getRole() == Role.MODERATOR) {
                    welcomeMessage = "Ожидают проверки новые профили и отзывы.";
                } else if (user.getRole() == Role.ADMIN) {
                    welcomeMessage = "Добро пожаловать в админ-панель!";
                }
                model.addAttribute("welcomeMessage", welcomeMessage);

            } catch (ClassCastException e) {
                log.warn("Failed to extract user details from authentication principal", e);
                model.addAttribute("isAuthenticated", false);
            }
        } else {
            model.addAttribute("isAuthenticated", false);
        }

        return "main";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("registrationForm", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("registrationForm") RegistrationForm form,
            BindingResult result,
            Model model
            //@RequestParam String username,
            //@RequestParam String password
    ) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userDetailsService.registerUser(form);
        } catch (Exception e) {
            log.warn("Registration failed for email {}: {}", form.getEmail(), e.getMessage());
            result.rejectValue("email", "error.registrationForm", "User with this email already exists");
            return "redirect:/register?error";
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(form.getEmail(), form.getPassword())
            );

            // Set the authentication in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.error("Auto-login failed after registration for email {}", form.getEmail(), e);
            return "redirect:/login";
        }
        // Redirect to the /login endpoint
        //return "redirect:/login?success";
        return "redirect:/greet"; // сразу на greet
    }
}