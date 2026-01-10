package com.example.telegram_bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;

@Configuration
@EnableScheduling
public class BotConfig {
    @Bean
    public TelegramClient telegramClient(@Value("${bot.token}") String botToken) {
        return new OkHttpTelegramClient(botToken);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
