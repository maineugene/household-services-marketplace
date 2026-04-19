package com.zhukovskiy.platform.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    // В будущем здесь будет @Id для базы данных
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;

    //@Column(unique = true, nullable = false)
    private String username; // Обычно это email
    //@Column(nullable = false)
    private String password;

    //private String firstName;
    //private String lastName;
    //private String country;
    //private Date dob;
}

//TODO лучше сделать дополнительно класс в пакете security CustomUserDetails implements UserDetails
//TODO и вызывать в UserDetailsService из него методы
//TODO решить проблему с PasswordEncoder, тк он может быть разным в CustomUserDetailsService и WebsecurityConfig

