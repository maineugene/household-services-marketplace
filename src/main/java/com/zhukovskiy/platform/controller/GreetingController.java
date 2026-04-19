package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.dto.RegistrationForm;
import com.zhukovskiy.platform.service.CustomUserDetailsService;
import jakarta.validation.Valid;
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
public class GreetingController {

    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;

    public GreetingController(CustomUserDetailsService userDetailsService, AuthenticationManager authenticationManager) {
        this.userDetailsService = userDetailsService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/")
    public String home(){
        return "index";
    }

    @GetMapping("/greet")
    public String greet(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = authentication.getName();
        System.out.println("Username from context " + username);

        // Add the username to the model
        model.addAttribute("username", username);
        //TODO также добавлять все другие атрибуты кроме пароля
        // Return the Thymeleaf template name
        return "greet";
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
            BindingResult result
            //@RequestParam String username,
            //@RequestParam String password
    ) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            userDetailsService.registerUser(form.getEmail(), form.getPassword());
        } catch (Exception userExistsAlready) {
            // Redirect to the /register endpoint
            return "redirect:/register?error";
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(form.getEmail(), form.getPassword())
        );

        // Set the authentication in the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Redirect to the /login endpoint
        //return "redirect:/login?success";
        return "redirect:/greet"; // сразу на greet
    }
}
