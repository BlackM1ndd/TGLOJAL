package com.example.entity;

import jakarta.persistence.*;

import java.io.Serializable;


@Entity
@Table(name ="users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Уникальный идентификатор пользователя
    private Long chatId;  // Идентификатор чата пользователя в Telegram
    @Column(unique = true)
    private String phoneNumber;  // Номер телефона пользователя
    private boolean isEmployee;  // Флаг, указывающий, является ли пользователь сотрудником
    private boolean isAdmin;  // Флаг, указывающий, является ли пользователь администратором
    private int points;  // Количество баллов, которые накопил пользователь
    @Enumerated(EnumType.STRING)
    private UserState userState;  // Состояние пользователя

    public User() {
    }

    public User(Long chatId, String phoneNumber, boolean isEmployee, boolean isAdmin, int points, UserState userState) {
        this.chatId = chatId;
        this.phoneNumber = phoneNumber;
        this.isEmployee = isEmployee;
        this.isAdmin = isAdmin;
        this.points = points;
        this.userState = userState;
    }

    public User(Long chatId, String phoneNumber, boolean isEmployee, boolean isAdmin) {
        this.chatId = chatId;
        this.phoneNumber = phoneNumber;
        this.isEmployee = isEmployee;
        this.isAdmin = isAdmin;
        this.points = 0;  // Баллы по умолчанию 0
        this.userState = UserState.DEFAULT;  // Состояние по умолчанию
    }


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

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public UserState getUserState() {
        return userState;
    }

    public void setUserState(UserState userState) {
        this.userState = userState;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", chatId=" + chatId +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", isEmployee=" + isEmployee +
                ", isAdmin=" + isAdmin +
                ", points=" + points +
                ", userState=" + userState +
                '}';
    }
}