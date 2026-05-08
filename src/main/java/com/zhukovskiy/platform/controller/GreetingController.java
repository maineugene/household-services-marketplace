package com.zhukovskiy.platform.controller;

import com.zhukovskiy.platform.dto.RegistrationForm;
import com.zhukovskiy.platform.model.User;
import com.zhukovskiy.platform.repository.UserRepository;
import com.zhukovskiy.platform.service.CustomUserDetails;
import com.zhukovskiy.platform.service.CustomUserDetailsService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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
public class GreetingController {

    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String home(){
        return "index";
    }

    @GetMapping("/greet")
    public String greet(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        model.addAttribute("user", user);
        /*model.addAttribute("email", user.getEmail());
        model.addAttribute("firstName", user.getFirstName());
        model.addAttribute("lastName", user.getLastName());
        model.addAttribute("dob", user.getDob());
        model.addAttribute("")*/
        String email = authentication.getName();
        System.out.println("Email from context " + email);

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
        } catch (Exception userExistsAlready) {
            // Redirect to the /register endpoint
            result.rejectValue("email", "error.registrationForm", "User with this email already exists");
            return "redirect:/register?error";
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(form.getEmail(), form.getPassword())
            );

            // Set the authentication in the SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e){
            return "redirect:/login";
        }
        // Redirect to the /login endpoint
        //return "redirect:/login?success";
        return "redirect:/greet"; // сразу на greet
    }
}
