package com.example.service;

import com.example.entity.User;
import com.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoyaltyService {
    private final UserRepository userRepository;

    public LoyaltyService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void addPoints(String userPhoneNumber, int points) {
        User user = userRepository.findByPhoneNumber(userPhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден."));
        user.setPoints(user.getPoints() + points);
        userRepository.save(user);
    }

    @Transactional
    public void redeemPoints(String phoneNumber, int points) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден по номеру телефона."));
        if (user.getPoints() >= points) {
            user.setPoints(user.getPoints() - points);
            userRepository.save(user);
        } else {
            throw new IllegalArgumentException("Недостаточно баллов для использования.");
        }
    }

    @Transactional
    public void addEmployee(String employeePhoneNumber) {
        User employee = userRepository.findByPhoneNumber(employeePhoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Сотрудник не найден."));
        employee.setEmployee(true);
        userRepository.save(employee);
    }
}
