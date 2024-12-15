package com.example.service;

import com.example.entity.User;
import com.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Сервис для управления пользователями.
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Проверяет, зарегистрирован ли пользователь по chatId.
     *
     * @param chatId идентификатор чата пользователя
     * @return true, если пользователь зарегистрирован; иначе false
     */
    public boolean isRegistered(long chatId) {
        return userRepository.findByChatId(chatId).isPresent();
    }

    /**
     * Проверяет, зарегистрирован ли номер телефона.
     *
     * @param phoneNumber номер телефона для проверки
     * @return true, если номер телефона зарегистрирован; иначе false
     */
    public boolean isPhoneNumberRegistered(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    /**
     * Регистрация нового пользователя.
     *
     * @param chatId идентификатор чата пользователя
     * @param phoneNumber номер телефона пользователя
     * @throws IllegalArgumentException если номер телефона уже зарегистрирован
     */
    @Transactional
    public void registerUser(long chatId, String phoneNumber) {
        if (isPhoneNumberRegistered(phoneNumber)) {
            logger.warn("Попытка регистрации с уже зарегистрированным номером: {}", phoneNumber);
            throw new IllegalArgumentException("Номер телефона уже зарегистрирован.");
        }

        User user = new User(chatId, phoneNumber, false, false, 0);
        userRepository.save(user);
        logger.info("Пользователь с chatId {} и номером телефона {} успешно зарегистрирован.", chatId, phoneNumber);
    }

    /**
     * Проверяет, является ли пользователь сотрудником по номеру телефона.
     *
     * @param phoneNumber номер телефона пользователя
     * @return true, если пользователь является сотрудником; иначе false
     */
    public boolean isEmployee(String phoneNumber) {
        return userRepository.existsByPhoneNumberAndIsEmployeeTrue(phoneNumber);
    }

    /**
     * Проверяет, является ли пользователь администратором по номеру телефона.
     *
     * @param phoneNumber номер телефона пользователя
     * @return true, если пользователь является администратором; иначе false
     */
    public boolean isAdmin(String phoneNumber) {
        return userRepository.existsByPhoneNumberAndIsAdminTrue(phoneNumber);
    }

    /**
     * Получает количество баллов пользователя по chatId.
     *
     * @param chatId идентификатор чата пользователя
     * @return количество баллов пользователя
     * @throws IllegalArgumentException если пользователь не найден
     */
    public int getUserPoints(long chatId) {
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));
        return user.getPoints();
    }

    /**
     * Получает номер телефона пользователя по chatId.
     *
     * @param chatId идентификатор чата пользователя
     * @return номер телефона пользователя
     * @throws IllegalArgumentException если пользователь не найден
     */
    public String getPhoneNumberByChatId(long chatId) {
        return userRepository.findByChatId(chatId)
                .map(User::getPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с таким chatId не найден."));
    }

}