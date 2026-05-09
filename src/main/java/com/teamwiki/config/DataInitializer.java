package com.teamwiki.config;

import com.teamwiki.entity.User;
import com.teamwiki.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@teamwiki.com")
                    .password(passwordEncoder.encode("admin123"))
                    .nickname("系统管理员")
                    .department("技术部")
                    .role(User.UserRole.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("已创建默认管理员账号: admin / admin123");

            User demo = User.builder()
                    .username("demo")
                    .email("demo@teamwiki.com")
                    .password(passwordEncoder.encode("demo123"))
                    .nickname("演示用户")
                    .department("产品部")
                    .role(User.UserRole.USER)
                    .enabled(true)
                    .build();
            userRepository.save(demo);
            log.info("已创建演示用户: demo / demo123");
        } else {
            User admin = userRepository.findByUsername("admin").orElse(null);
            if (admin != null && admin.getRole() != User.UserRole.ADMIN) {
                admin.setRole(User.UserRole.ADMIN);
                userRepository.save(admin);
                log.info("已将admin用户升级为管理员");
            }
        }
    }
}
