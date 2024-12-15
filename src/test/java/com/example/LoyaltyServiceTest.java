package com.example;

//import com.example.entity.User;
//import com.example.repository.UserRepository;
//import com.example.service.LoyaltyService;
//import org.junit.jupiter.api.*;
//
//import java.util.Optional;
//
//import static org.mockito.Mockito.*;
//import static org.junit.jupiter.api.Assertions.*;
//
//class LoyaltyServiceTest {
//
//    private UserRepository userRepository;
//    private LoyaltyService loyaltyService;
//
//    @BeforeEach
//    void setUp() {
//        userRepository = mock(UserRepository.class);
//        loyaltyService = new LoyaltyService(userRepository);
//    }
//
//    @Test
//    void testAddPoints() throws Exception {
//        User user = new User(1L);
//        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
//        when(userRepository.isEmployee(2L)).thenReturn(true);
//
//        loyaltyService.addPoints(2L, 1L, 10);
//
//        assertEquals(10, user.getPoints());
//        verify(userRepository).save(user);
//    }
//
//    @Test
//    void testRedeemPoints() throws Exception {
//        User user = new User(1L);
//        user.addPoints(20);
//        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
//
//        loyaltyService.redeemPoints(1L);
//
//        assertEquals(10, user.getPoints());
//        verify(userRepository).save(user);
//    }
//
//    @Test
//    void testRedeemPointsNotEnough() {
//        User user = new User(1L);
//        user.addPoints(5);
//        when(userRepository.findUserById(1L)).thenReturn(Optional.of(user));
//
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> loyaltyService.redeemPoints(1L));
//        assertEquals("Недостаточно баллов для обмена.", exception.getMessage());
//    }
//}
