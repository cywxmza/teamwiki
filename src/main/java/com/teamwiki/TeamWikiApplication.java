package com.teamwiki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TeamWikiApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeamWikiApplication.class, args);
    }
}
