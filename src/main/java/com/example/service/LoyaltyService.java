package com.example.service;

import com.example.entity.User;
import com.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoyaltyService {
    private static final Logger logger = LoggerFactory.getLogger(LoyaltyService.class);

    private final UserRepository userRepository;

    public LoyaltyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void addPoints(String employeePhoneNumber, String userPhoneNumber, int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Количество баллов не может быть отрицательным.");
        }

        if (!userRepository.existsByPhoneNumberAndIsEmployeeTrue(employeePhoneNumber)) {
            logger.warn("Попытка начисления баллов без прав: {}", employeePhoneNumber);
            throw new IllegalArgumentException("Нет прав для начисления баллов.");
        }

        User user = userRepository.findByPhoneNumber(userPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));

        user.addPoints(points);
        userRepository.save(user);
        logger.info("Баллы успешно начислены пользователю: {}. Количество: {}", userPhoneNumber, points);
    }

    @Transactional
    public void redeemPoints(long chatId, int points) {
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));

        if (user.getPoints() < points) {
            throw new IllegalArgumentException("Недостаточно баллов для списания.");
        }

        user.subtractPoints(points);
        userRepository.save(user);
        logger.info("{} баллов списаны у пользователя с chatId: {}", points, chatId);
    }


    @Transactional
    public void addEmployee(String adminPhoneNumber, String employeePhoneNumber) {
        if (!userRepository.existsByPhoneNumberAndIsAdminTrue(adminPhoneNumber)) {
            logger.warn("Попытка добавления сотрудника без прав: {}", adminPhoneNumber);
            throw new IllegalArgumentException("Нет прав для добавления сотрудника.");
        }

        User employee = userRepository.findByPhoneNumber(employeePhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));

        employee.setEmployee(true);
        userRepository.save(employee);
        logger.info("Сотрудник добавлен: {}", employeePhoneNumber);
    }
    @Transactional
    public void removeEmployee(String adminPhoneNumber, String employeePhoneNumber) {
        if (!userRepository.existsByPhoneNumberAndIsAdminTrue(adminPhoneNumber)) {
            logger.warn("Попытка добавления сотрудника без прав: {}", adminPhoneNumber);
            throw new IllegalArgumentException("Нет прав для добавления сотрудника.");
        }
        User employee = userRepository.findByPhoneNumber(employeePhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));

        employee.setEmployee(false);
        userRepository.save(employee);
        logger.info("Сотрудник удален: {}", employeePhoneNumber);
    }
}