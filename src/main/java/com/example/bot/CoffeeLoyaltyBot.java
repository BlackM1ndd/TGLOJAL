package com.example.bot;

import com.example.entity.UserState;
import com.example.service.UserService;
import com.example.service.LoyaltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

@Component
public class CoffeeLoyaltyBot extends TelegramLongPollingBot {

    private final UserService userService;
    private final LoyaltyService loyaltyService;

    @Autowired
    public CoffeeLoyaltyBot(UserService userService, LoyaltyService loyaltyService) {
        this.userService = userService;
        this.loyaltyService = loyaltyService;
    }

    private final Map<UserState, BiConsumer<Long, String>> stateHandlers = Map.of(
            UserState.ADD_POINTS_AWAITING_PHONE, this::handleAddPoints,
            UserState.ADD_POINTS_AWAITING_AMOUNT, this::handleAddPoints,
            UserState.REDEEM_AWAITING_PHONE, this::handleRedeem,
            UserState.REDEEM_AWAITING_AMOUNT, this::handleRedeem,
            UserState.ADD_EMPLOYEE_AWAITING_PHONE, this::handleAddEmployee
    );

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();
            String command = mapCommand(messageText.split(" ")[0]);

            try {
                if (!userService.isRegistered(chatId)
                        && !(command.equals("/start") || command.equals("/help") || command.equals("/register"))) {
                    sendMessage(chatId, getMessage("register_first"));
                    return;
                }
                if (userService.getUserState(chatId) != UserState.DEFAULT) {
                    handleUserMessage(chatId, messageText);
                    return;
                }
                executeCommand(command, chatId);
            } catch (Exception e) {
                sendMessage(chatId, getMessage("error_generic") + e.getMessage());
            }
        }
    }

    private String mapCommand(String input) {
        return switch (input.toLowerCase()) {
            case "помощь", "/помощь" -> "/help";
            case "старт", "/старт" -> "/start";
            case "регистрация", "/регистрация" -> "/register";
            case "баланс", "/баланс" -> "/balance";
            case "добавить баллы", "/добавитьбаллы" -> "/addpoints";
            case "списать баллы", "/списатьбаллы" -> "/redeem";
            case "добавить сотрудника", "/добавитьсотрудника" -> "/addemployee";
            default -> input;
        };
    }

    private void executeCommand(String command, long chatId) {
        switch (command) {
            case "/start" -> handleStart(chatId);
            case "/help" -> {
                if (userService.isRegistered(chatId)) {
                    handleHelp(chatId);
                } else {
                    sendMessage(chatId, getMessage("register_first"));
                }
            }
            case "/register" -> handleRegister(chatId);
            case "/balance", "/addpoints", "/redeem", "/addemployee" -> {
                if (!userService.isRegistered(chatId)) {
                    sendMessage(chatId, getMessage("register_first"));
                } else {
                    switch (command) {
                        case "/balance" -> handleBalance(chatId);
                        case "/addpoints" -> initiateAddPoints(chatId);
                        case "/redeem" -> initiateRedeem(chatId);
                        case "/addemployee" -> initiateAddEmployee(chatId);
                    }
                }
            }
            default -> sendMessage(chatId, getMessage("unknown_command"));
        }
    }

    private void handleUserMessage(long chatId, String userInput) {
        UserState state = userService.getUserState(chatId);
        BiConsumer<Long, String> handler = stateHandlers.get(state);
        if (handler != null) {
            handler.accept(chatId, userInput);
        } else {
            sendMessage(chatId, getMessage("unexpected_message"));
        }
    }

    private void handleStart(long chatId) {
        if (!userService.isRegistered(chatId)) {
            sendMessage(chatId, getMessage("start_message_unregistered"));
        } else {
            sendMessage(chatId, getMessage("start_message_registered"));
        }
    }

    private void handleHelp(long chatId) {
        boolean isEmployee = userService.isEmployee(chatId);
        boolean isAdmin = userService.isAdmin(chatId);

        System.out.println("User roles for chatId " + chatId + ": isEmployee=" + isEmployee + ", isAdmin=" + isAdmin);
        StringBuilder helpMessage = new StringBuilder(getMessage("help_base"));
        helpMessage.append(getMessage("help_common"));
        if (isEmployee || isAdmin) {
            helpMessage.append(getMessage("help_employee"));
        }
        if (isAdmin) {
            helpMessage.append(getMessage("help_admin"));
        }
        sendMessage(chatId, helpMessage.toString());
    }

    private void handleRegister(long chatId) {
        if (userService.isRegistered(chatId)) {
            sendMessage(chatId, getMessage("already_registered"));
            return;
        }
        UserState currentState = userService.getUserState(chatId);
        if (currentState == UserState.AWAITING_PHONE) {
            sendMessage(chatId, getMessage("register_in_progress"));
            return;
        }
        startDialog(chatId, UserState.AWAITING_PHONE, "Пожалуйста, введите свой номер телефона для регистрации.");
    }

    private void handleBalance(long chatId) {
        int points = userService.getUserPoints(chatId);
        sendMessage(chatId, String.format(getMessage("balance"), points));
    }

    private void initiateAddPoints(long chatId) {
        startDialog(chatId, UserState.ADD_POINTS_AWAITING_PHONE, getMessage("add_points_prompt"));
    }

    private void handleAddPoints(long chatId, String userInput) {
        UserState state = userService.getUserState(chatId);

        if (state == UserState.ADD_POINTS_AWAITING_PHONE) {
            userService.setTemporaryData(chatId, "phone", userInput.trim());
            startDialog(chatId, UserState.ADD_POINTS_AWAITING_AMOUNT, getMessage("add_points_amount"));
        } else if (state == UserState.ADD_POINTS_AWAITING_AMOUNT) {
            try {
                int points = Integer.parseInt(userInput.trim());
                String phoneNumber = userService.getTemporaryData(chatId, "phone");
                loyaltyService.addPoints(phoneNumber, points);

                // Уведомление инициатора о завершении операции
                sendMessage(chatId, String.format(getMessage("points_added"), points, phoneNumber));

                // Уведомление пользователя, которому начислены баллы
                Long recipientChatId = userService.getChatIdByPhoneNumber(phoneNumber);
                if (recipientChatId != null) {
                    int newBalance = userService.getUserPoints(recipientChatId);
                    sendMessage(recipientChatId, String.format(getMessage("points_received"), points, newBalance));
                }
            } catch (NumberFormatException e) {
                sendMessage(chatId, getMessage("invalid_number"));
            } finally {
                userService.setUserState(chatId, UserState.DEFAULT);
            }
        }
    }

    private void initiateRedeem(long chatId) {
        startDialog(chatId, UserState.REDEEM_AWAITING_PHONE, getMessage("redeem_points_prompt"));
    }

    private void handleRedeem(long chatId, String userInput) {
        UserState state = userService.getUserState(chatId);

        if (state == UserState.REDEEM_AWAITING_PHONE) {
            userService.setTemporaryData(chatId, "phone", userInput.trim());
            startDialog(chatId, UserState.REDEEM_AWAITING_AMOUNT, getMessage("redeem_points_amount"));
        } else if (state == UserState.REDEEM_AWAITING_AMOUNT) {
            try {
                int points = Integer.parseInt(userInput.trim());
                String phoneNumber = userService.getTemporaryData(chatId, "phone");

                if (phoneNumber == null) {
                    sendMessage(chatId, getMessage("phone_not_set"));
                    return;
                }

                loyaltyService.redeemPoints(phoneNumber, points);

                sendMessage(chatId, String.format(getMessage("points_redeemed"), points, phoneNumber));
                Long recipientChatId = userService.getChatIdByPhoneNumber(phoneNumber);
                if (recipientChatId != null) {
                    int newBalance = userService.getUserPoints(recipientChatId);
                    sendMessage(recipientChatId, String.format(getMessage("points_deducted"), points, newBalance));
                }
            } catch (NumberFormatException e) {
                sendMessage(chatId, getMessage("invalid_number"));
            } finally {
                userService.setUserState(chatId, UserState.DEFAULT);
            }
        }
    }

    private void initiateAddEmployee(long chatId) {
        startDialog(chatId, UserState.ADD_EMPLOYEE_AWAITING_PHONE, getMessage("add_employee_prompt"));
    }

    private void handleAddEmployee(long chatId, String userInput) {
        try {
            loyaltyService.addEmployee(userInput.trim());
            sendMessage(chatId, String.format(getMessage("employee_added"), userInput));
        } catch (IllegalArgumentException e) {
            sendMessage(chatId, e.getMessage());
        } finally {
            userService.setUserState(chatId, UserState.DEFAULT);
        }
    }

    private void startDialog(long chatId, UserState newState, String message) {
        sendMessage(chatId, message);
        userService.setUserState(chatId, newState);
    }

    private String getMessage(String key) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.getDefault());
        return bundle.getString(key);
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "Tg4490_bot";
    }

    @Override
    public String getBotToken() {
        return "7370408312:AAEorSyiTIGGvJ1mSPpoL6lziEvFWLK46YU";
    }
}