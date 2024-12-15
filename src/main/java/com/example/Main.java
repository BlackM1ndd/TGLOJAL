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

        // Проверка наличия класса драйвера
        checkDatabaseDriverClass();

        SpringApplication.run(Main.class, args);
    }

    private static void checkDatabaseDriverClass() {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Драйвер базы данных найден.");
        } catch (ClassNotFoundException e) {
            System.err.println("Ошибка: Драйвер базы данных не найден. Убедитесь, что зависимость драйвера добавлена в ваш проект.");
            e.printStackTrace();
            throw new RuntimeException("Драйвер базы данных не найден", e);
        }
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