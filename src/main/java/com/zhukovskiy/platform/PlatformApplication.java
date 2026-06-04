package com.zhukovskiy.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class PlatformApplication {
	public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
	}
}
