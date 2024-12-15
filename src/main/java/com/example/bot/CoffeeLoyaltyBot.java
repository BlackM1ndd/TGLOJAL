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
import java.util.List;
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
                    case "/removeemployee", "удалитьсотрудника" -> handleRemoveEmployee(chatId, args);
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
        StringBuilder helpMessage = new StringBuilder("Доступные команды:\n");

        // Проверяем статус пользователя
        if (!userService.isRegistered(chatId)) {
            // Незарегистрированный пользователь
            helpMessage.append("/start или старт - начать использование бота\n")
                    .append("/help или помощь - список доступных команд\n")
                    .append("/register или регистрация - регистрация пользователя\n");
        } else {
            // Зарегистрированный пользователь
            helpMessage.append("/start или старт - начать использование бота\n")
                    .append("/help или помощь - список доступных команд\n")
                    .append("/balance или баланс - узнать баланс баллов\n");

            // Добавляем информацию о текущей акции
            helpMessage.append("🎉 Акция! 🎉\n")
                    .append("Купите 10 кружек кофе и получите одну кружку в подарок!\n");

            if (userService.isEmployee(userService.getPhoneNumberByChatId(chatId))) {
                // Сотрудник
                helpMessage.append("/addpoints или добавитьбаллы - добавить баллы клиенту\n")
                        .append("/redeem или списать - списать баллы\n");
            }

            if (userService.isAdmin(userService.getPhoneNumberByChatId(chatId))) {
                // Администратор
                helpMessage.append("/addemployee или добавитьсотрудника - назначить сотрудника\n")
                        .append("/removeemployee или удалитьсотрудника - удалить сотрудника\n");
            }
        }

        // Отправляем сформированное сообщение
        sendMessage(chatId, helpMessage.toString());
    }

    private void handleState(long chatId, String messageText) {
        UserState state = userStates.get(chatId);

        switch (state) {
            case AWAITING_PHONE -> handleAwaitingPhone(chatId, messageText);
            case ADD_POINTS_AWAITING_PHONE -> handleAddPointsAwaitingPhone(chatId, messageText);
            case ADD_POINTS_AWAITING_AMOUNT -> handleAddPointsAwaitingAmount(chatId, messageText);
            case REDEEM_AWAITING_PHONE -> handleRedeemAwaitingPhone(chatId, messageText);
            case REDEEM_AWAITING_AMOUNT -> handleRedeemAwaitingAmount(chatId, messageText);
            case ADD_EMPLOYEE_AWAITING_PHONE -> handleAddEmployeeAwaitingPhone(chatId, messageText);
            case REMOVE_EMPLOYEE_AWAITING_PHONE ->handleRemoveEmployeeAwaitingPhone(chatId, messageText);
        }
    }

    private void handleAwaitingPhone(long chatId, String messageText) {
        try {
            userService.registerUser(chatId, messageText);
            sendMessage(chatId, "Вы успешно зарегистрированы!");
            String promotionMessage = "🎉 Акция! 🎉\n" +
                    "Купите 10 кружек кофе и получите одну кружку в подарок! " +
                    "Не упустите возможность насладиться своим любимым напитком!" +
                    "Нажми /help для просмотра доступных команд";
            sendMessage(chatId, promotionMessage);
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка регистрации: " + e.getMessage());
        } finally {
            userStates.remove(chatId);
        }
    }

    private void handleAddPointsAwaitingPhone(long chatId, String messageText) {
        // Сохраняем номер телефона клиента
        tempData.put(chatId, messageText);
        sendMessage(chatId, "Введите количество баллов для начисления:");
        userStates.put(chatId, UserState.ADD_POINTS_AWAITING_AMOUNT);
    }

    private void handleAddPointsAwaitingAmount(long chatId, String messageText) {
        try {
            String employeePhoneNumber = userService.getPhoneNumberByChatId(chatId); // Номер телефона сотрудника
            String userPhoneNumber = tempData.get(chatId); // Номер телефона клиента
            int points = Integer.parseInt(messageText); // Количество баллов для начисления

            // Вызываем метод для начисления баллов
            loyaltyService.addPoints(employeePhoneNumber, userPhoneNumber, points);

            // Уведомляем клиента о начислении баллов
            long userChatId = userService.getChatIdByPhoneNumber(userPhoneNumber); // Получаем chatId клиента
            sendMessage(userChatId, "Вам начислено " + points + " баллов. Спасибо за использование наших услуг!");

            // Уведомляем администраторов о начислении баллов
            String notificationToAdmin = String.format("Сотрудник (номер: %s) начислил %d баллов клиенту (номер: %s).",
                    employeePhoneNumber, points, userPhoneNumber);

            // Получаем список администраторов
            List<Long> adminChatIds = userService.getAdminChatIds();
            for (Long adminChatId : adminChatIds) {
                sendMessage(adminChatId, notificationToAdmin); // Теперь adminChatId - long
            }

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
        // Проверяем, является ли пользователь администратором или сотрудником
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

        // Запрашиваем номер телефона у клиента
        sendMessage(chatId, "Введите номер телефона клиента для списания баллов:");
        userStates.put(chatId, UserState.REDEEM_AWAITING_PHONE);
    }

    private void handleRedeemAwaitingPhone(long chatId, String messageText) {
        // Сохраняем номер телефона клиента
        tempData.put(chatId, messageText);
        sendMessage(chatId, "Введите количество баллов для списания (максимум 30):");
        userStates.put(chatId, UserState.REDEEM_AWAITING_AMOUNT);
    }

    private void handleRedeemAwaitingAmount(long chatId, String messageText) {
        try {
            int points = Integer.parseInt(messageText);
            if (points <= 0 || points > 30) {
                sendMessage(chatId, "Количество баллов должно быть от 1 до 30.");
                return;
            }

            String userPhoneNumber = tempData.get(chatId); // Получаем номер телефона клиента
            long userChatId = userService.getChatIdByPhoneNumber(userPhoneNumber); // Получаем chatId клиента по его номеру телефона

            // Вызываем метод для списания баллов
            loyaltyService.redeemPoints(userChatId, points);

            // Уведомляем клиента о списании баллов
            sendMessage(userChatId, points + " баллов были списаны с вашего счета. Спасибо за использование наших услуг!");

            // Уведомляем администраторов о списании баллов
            String employeePhoneNumber = userService.getPhoneNumberByChatId(chatId); // Номер телефона сотрудника
            String notificationToAdmin = String.format("Сотрудник (номер: %s) списал %d баллов у клиента (номер: %s).",
                    employeePhoneNumber, points, userPhoneNumber);

            // Получаем список администраторов
            List<Long> adminChatIds = userService.getAdminChatIds();
            for (Long adminChatId : adminChatIds) {
                sendMessage(adminChatId, notificationToAdmin); // Теперь adminChatId - long
            }

        } catch (NumberFormatException e) {
            sendMessage(chatId, "Количество баллов должно быть числом.");
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        } finally {
            userStates.remove(chatId);
            tempData.remove(chatId);
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
    private void handleRemoveEmployee(long chatId, String[] args) {
        // Проверяем, что пользователь является администратором
        String adminPhoneNumber = userService.getPhoneNumberByChatId(chatId);
        if (!userService.isAdmin(adminPhoneNumber)) {
            sendMessage(chatId, "Эта команда доступна только для администраторов.");
            return;
        }

        if (args.length < 2) {
            sendMessage(chatId, "Введите номер телефона сотрудника для удаления:");
            userStates.put(chatId, UserState.REMOVE_EMPLOYEE_AWAITING_PHONE);
            return;
        }

        // Если номер телефона уже был передан, удаляем сотрудника
        String employeePhoneNumber = args[1];
        handleRemoveEmployeeAwaitingPhone(chatId, employeePhoneNumber);
    }

    private void handleRemoveEmployeeAwaitingPhone(long chatId, String messageText) {
        String adminPhoneNumber = userService.getPhoneNumberByChatId(chatId);

        try {
            // Удаляем сотрудника
            loyaltyService.removeEmployee(adminPhoneNumber, messageText);
            sendMessage(chatId, "Сотрудник успешно удален: " + messageText);
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка: " + e.getMessage());
        } finally {
            userStates.remove(chatId); // Убираем состояние, чтобы не ожидать ввод дальше
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