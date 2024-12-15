package com.example.repository;

import com.example.entity.User;
import com.example.entity.UserState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByChatId(Long chatId);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Optional<User> findByChatIdAndUserState(Long chatId, UserState userState);

    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumberAndIsEmployeeTrue(String phoneNumber);
    boolean existsByPhoneNumberAndIsAdminTrue(String phoneNumber);


}
