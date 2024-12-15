package com.example.service;

import com.example.entity.User;
import com.example.entity.UserState;
import com.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Map<Long, Map<String, String>> temporaryData = new HashMap<>();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isRegistered(long chatId) {
        return userRepository.findByChatId(chatId).isPresent();
    }

    public boolean isPhoneNumberRegistered(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }


    @Transactional
    public void registerUser(long chatId, String phoneNumber) {
        if (isRegistered(chatId)) {
            throw new IllegalArgumentException("Пользователь с этим chatId уже зарегистрирован.");
        }

        if (isPhoneNumberRegistered(phoneNumber)) {
            throw new IllegalArgumentException("Номер телефона уже зарегистрирован.");
        }

        User user = new User(chatId, phoneNumber, false, false, 0, UserState.DEFAULT);
        userRepository.save(user);
    }

    public boolean isEmployee(long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::isEmployee)
                .orElse(false);
    }

    public boolean isAdmin(long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::isAdmin)
                .orElse(false);
    }

    public int getUserPoints(long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::getPoints)
                .orElse(0);
    }

    public UserState getUserState(long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::getUserState)
                .orElse(UserState.DEFAULT);
    }

    @Transactional
    public void setUserState(long chatId, UserState userState) {
        userRepository.findByChatId(chatId).ifPresent(user -> {
            user.setUserState(userState);
            userRepository.save(user);
        });
    }

    public void setTemporaryData(Long chatId, String key, String value) {
        temporaryData.computeIfAbsent(chatId, k -> new HashMap<>()).put(key, value);
    }

    public String getTemporaryData(Long chatId, String key) {
        return temporaryData.getOrDefault(chatId, new HashMap<>()).get(key);
    }

    public Long getChatIdByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(User::getChatId)
                .orElse(null);
    }

    @Transactional
    public void updateUserPoints(long chatId, int points) {
        userRepository.findByChatId(chatId).ifPresent(user -> {
            user.setPoints(user.getPoints() + points);
            userRepository.save(user);
        });
    }
}
