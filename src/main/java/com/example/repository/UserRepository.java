package com.example.repository;

import com.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByChatId(long chatId);

    Optional<User> findByPhoneNumber(String phoneNumber); // Добавьте этот метод

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIsEmployeeTrue(String phoneNumber);

    boolean existsByPhoneNumberAndIsAdminTrue(String phoneNumber);
}