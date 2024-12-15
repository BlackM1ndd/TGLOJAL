package com.example.entity;

import jakarta.persistence.*;

/**
 * Класс, представляющий пользователя в системе.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private Long chatId;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    @Column(name = "points", nullable = false)
    private int points = 0;

    @Column(name = "is_employee", nullable = false)
    private boolean isEmployee = false;

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin = false;

    public User() {}

    public User(Long chatId, String phoneNumber, boolean isEmployee, boolean isAdmin, int points) {
        this.chatId = chatId;
        this.phoneNumber = phoneNumber;
        this.isEmployee = isEmployee;
        this.isAdmin = isAdmin;
        this.points = points;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isEmployee() {
        return isEmployee;
    }

    public void setEmployee(boolean employee) {
        isEmployee = employee;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    /**
     * Метод для добавления баллов на счет пользователя.
     *
     * @param pointsToAdd количество баллов для добавления
     */
    public void addPoints(int pointsToAdd) {
        if (pointsToAdd < 0) {
            throw new IllegalArgumentException("Количество добавляемых баллов не может быть отрицательным.");
        }
        this.points += pointsToAdd;
    }

    /**
     * Метод для списания баллов с счета пользователя.
     *
     * @param pointsToRedeem количество баллов для списания
     * @return true, если списание успешно; иначе false
     */
    public boolean redeemPoints(int pointsToRedeem) {
        if (pointsToRedeem < 0) {
            throw new IllegalArgumentException("Количество списываемых баллов не может быть отрицательным.");
        }
        if (this.points >= pointsToRedeem) {
            this.points -= pointsToRedeem;
            return true;
        } else {
            return false;
        }
    }
    public void subtractPoints(int pointsToSubtract) {
        if (pointsToSubtract < 0) {
            throw new IllegalArgumentException("Количество списываемых баллов не может быть отрицательным.");
        }
        this.points -= pointsToSubtract;
    }
}