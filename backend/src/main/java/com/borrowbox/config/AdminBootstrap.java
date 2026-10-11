package com.borrowbox.config;

import com.borrowbox.entity.AppUser;
import com.borrowbox.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrap {
    @Bean
    CommandLineRunner ensureConfiguredAdmin(UserRepository users, PasswordEncoder encoder,
            @Value("${ADMIN_EMAIL:}") String adminEmail, @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        return args -> {
            if (adminEmail.isBlank() || adminPassword.isBlank()) return;
            if (adminPassword.length() < 12 || adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
                throw new IllegalStateException("ADMIN_PASSWORD must contain 12-72 UTF-8 bytes");
            String email = adminEmail.trim().toLowerCase();
            AppUser admin = users.findByEmail(email).orElseGet(() -> users.save(
                    new AppUser("BorrowBox Admin", email, encoder.encode(adminPassword), null)));
            admin.promoteToAdmin();
            users.save(admin);
        };
    }
}
