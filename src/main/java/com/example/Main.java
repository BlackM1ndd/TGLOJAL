package com.example;

import com.example.bot.CoffeeLoyaltyBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Locale;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("ru", "RU"));
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(CoffeeLoyaltyBot coffeeLoyaltyBot) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(coffeeLoyaltyBot);
            return telegramBotsApi;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException("Ошибка при регистрации бота", e);
        }
    }
}