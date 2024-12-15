package com.example.bot;

import com.example.entity.UserState;
import com.example.service.UserService;
import com.example.service.LoyaltyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.Map;

@Component
public class CoffeeLoyaltyBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(CoffeeLoyaltyBot.class);

    private final UserService userService;
    private final LoyaltyService loyaltyService;

    private final Map<Long, UserState> userStates = new HashMap<>();
    private final Map<Long, String> tempData = new HashMap<>();

    public CoffeeLoyaltyBot(UserService userService, LoyaltyService loyaltyService) {
        this.userService = userService;
        this.loyaltyService = loyaltyService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (userStates.containsKey(chatId)) {
                handleState(chatId, messageText);
                return;
            }

            String[] args = messageText.split(" ");
            String command = args[0].toLowerCase();

            try {
                switch (command) {
                    case "/start", "старт" -> handleStart(chatId);
                    case "/help", "помощь" -> handleHelp(chatId);
                    case "/register", "регистрация" -> handleRegister(chatId);
                    case "/balance", "баланс" -> handleBalance(chatId);
                    case "/addpoints", "добавитьбаллы" -> initAddPoints(chatId);
                    case "/redeem", "списать" -> handleRedeem(chatId);
                    case "/addemployee", "добавитьсотрудника" -> handleAddEmployee(chatId, args);
                    default -> sendMessage(chatId, "Неизвестная команда. Используйте /help для списка доступных команд.");
                }
            } catch (Exception e) {
                logger.error("Ошибка обработки команды: {}", e.getMessage());
                sendMessage(chatId, "Ошибка: " + e.getMessage());
            }
        }
    }

    private void handleStart(long chatId) {
        sendMessage(chatId, "Добро пожаловать в Coffee Loyalty! Используйте /help для списка доступных команд.");
    }

    private void handleHelp(long chatId) {
        String helpMessage = """
                Доступные команды:
                /start или старт - начать использование бота
                /help или помощь - список доступных команд
                /register или регистрация - регистрация пользователя
                /balance или баланс - узнать баланс баллов
                /addpoints или добавитьбаллы - добавить баллы клиенту (только для сотрудников)
                /redeem или списать - списать 10 баллов (одна порция кофе)
                /addemployee или добавитьсотрудника <номер администратора> <номер сотрудника> - назначить сотрудника (только для администраторов)
                """;
        sendMessage(chatId, helpMessage);
    }

    private void handleState(long chatId, String messageText) {
        UserState state = userStates.get(chatId);

        switch (state) {
            case AWAITING_PHONE -> handleAwaitingPhone(chatId, messageText);
            case ADD_POINTS_AWAITING_PHONE -> handleAddPointsAwaitingPhone(chatId, messageText);
            case ADD_POINTS_AWAITING_AMOUNT -> handleAddPointsAwaitingAmount(chatId, messageText);
            case REDEEM_AWAITING_AMOUNT -> handleRedeemAwaitingAmount(chatId, messageText);
            case ADD_EMPLOYEE_AWAITING_PHONE -> handleAddEmployeeAwaitingPhone(chatId, messageText);
        }
    }

    private void handleAwaitingPhone(long chatId, String messageText) {
        try {
            userService.registerUser(chatId, messageText);
            sendMessage(chatId, "Вы успешно зарегистрированы!");
            String promotionMessage = "🎉 Акция! 🎉\n" +
                    "Купите 10 кружек кофе и получите одну кружку в подарок! " +
                    "Не упустите возможность насладиться своим любимым напитком!";
            sendMessage(chatId, promotionMessage);
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка регистрации: " + e.getMessage());
        } finally {
            userStates.remove(chatId);
        }
    }

    private void handleAddPointsAwaitingPhone(long chatId, String messageText) {
        tempData.put(chatId, messageText);
        sendMessage(chatId, "Введите количество баллов для начисления:");
        userStates.put(chatId, UserState.ADD_POINTS_AWAITING_AMOUNT);
    }

    private void handleAddPointsAwaitingAmount(long chatId, String messageText) {
        try {
            String employeePhoneNumber = userService.getPhoneNumberByChatId(chatId); // Предполагается, что этот метод возвращает номер телефона
            String userPhoneNumber = tempData.get(chatId);
            int points = Integer.parseInt(messageText);
            loyaltyService.addPoints(employeePhoneNumber, userPhoneNumber, points);
            sendMessage(chatId, "Баллы успешно начислены.");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Количество баллов должно быть числом.");
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        } finally {
            userStates.remove(chatId);
            tempData.remove(chatId);
        }
    }

    private void handleRegister(long chatId) {
        sendMessage(chatId, "Введите номер телефона для регистрации:");
        userStates.put(chatId, UserState.AWAITING_PHONE);
    }

    private void handleBalance(long chatId) {
        try {
            int points = userService.getUserPoints(chatId);
            sendMessage(chatId, "Ваш баланс: " + points + " баллов.");
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        }
    }

    private void initAddPoints(long chatId) {
        // Проверяем, является ли пользователь сотрудником
        String phoneNumber = userService.getPhoneNumberByChatId(chatId);
        if (!userService.isAdmin(phoneNumber) && !userService.isEmployee(phoneNumber)) {
            sendMessage(chatId, "Эта команда доступна только для администраторов и сотрудников.");
            return;
        }

        // Отправляем сообщение с просьбой ввести номер телефона клиента
        sendMessage(chatId, "Введите номер телефона клиента для начисления баллов:");

        // Сохраняем состояние пользователя, чтобы узнать, что он вводит номер телефона
        userStates.put(chatId, UserState.ADD_POINTS_AWAITING_PHONE);
    }

    private void handleRedeem(long chatId) {
        // Проверяем, что пользователь является администратором или сотрудником
        String phoneNumber = userService.getPhoneNumberByChatId(chatId);
        if (!userService.isAdmin(phoneNumber) && !userService.isEmployee(phoneNumber)) {
            sendMessage(chatId, "Эта команда доступна только для администраторов и сотрудников.");
            return;
        }

        sendMessage(chatId, "Введите количество баллов для списания (50 баллов за раз):");
        userStates.put(chatId, UserState.REDEEM_AWAITING_AMOUNT);
    }

    private void handleRedeemAwaitingAmount(long chatId, String messageText) {
        try {
            int points = Integer.parseInt(messageText);
            if (points != 50) {
                sendMessage(chatId, "Вы можете списать только 50 баллов за раз.");
                return;
            }

            loyaltyService.redeemPoints(chatId);
            sendMessage(chatId, "10 баллов списаны. Наслаждайтесь своим кофе!");
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Количество баллов должно быть числом.");
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        } finally {
            userStates.remove(chatId);
        }
    }

    private void handleAddEmployee(long chatId, String[] args) {
        // Проверяем, что пользователь является администратором
        String adminPhoneNumber = userService.getPhoneNumberByChatId(chatId);
        if (!userService.isAdmin(adminPhoneNumber)) {
            sendMessage(chatId, "Эта команда доступна только для администраторов.");
            return;
        }

        if (args.length < 2) {
            sendMessage(chatId, "Введите номер телефона сотрудника для добавления:");
            userStates.put(chatId, UserState.ADD_EMPLOYEE_AWAITING_PHONE);
            return;
        }

        // Если номер телефона уже был передан, добавляем сотрудника
        String employeePhoneNumber = args[1];
        handleAddEmployeeAwaitingPhone(chatId, employeePhoneNumber);
    }

    private void handleAddEmployeeAwaitingPhone(long chatId, String messageText) {
        String adminPhoneNumber = userService.getPhoneNumberByChatId(chatId);

        try {
            // Добавляем нового сотрудника
            loyaltyService.addEmployee(adminPhoneNumber, messageText);
            sendMessage(chatId, "Сотрудник успешно добавлен: " + messageText);
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        } finally {
            userStates.remove(chatId); // Убираем состояние, чтобы не ожидать ввод дальше
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Ошибка отправки сообщения: {}", e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return "Tg4490_bot"; // Замените на имя вашего бота
    }

    @Override
    public String getBotToken() {
        return "7370408312:AAEorSyiTIGGvJ1mSPpoL6lziEvFWLK46YU"; // Замените на токен вашего бота
    }
}