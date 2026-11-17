package edu.sjsu.cmpe172.tutoring.service;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.sjsu.cmpe172.tutoring.model.User;
import edu.sjsu.cmpe172.tutoring.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordService passwordService;
    @InjectMocks UserService service;

    @Test
    void registerUser_invalidRole_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.registerUser("a", "pw", "ADMIN", null));
        verify(userRepository, never()).saveUser(any(), any(), any());
    }

    @Test
    void registerUser_duplicateUsername_isRejected() {
        when(userRepository.findByUsername("a")).thenReturn(new User(1L, "a", "h", "CUSTOMER"));
        assertThrows(IllegalArgumentException.class, () -> service.registerUser("a", "pw", "CUSTOMER", null));
        verify(userRepository, never()).saveUser(any(), any(), any());
    }

    @Test
    void registerUser_customer_storesHashAndNoProviderProfile() {
        when(userRepository.findByUsername("a")).thenReturn(null);
        when(passwordService.hashPassword("pw")).thenReturn("hash");
        when(userRepository.saveUser("a", "hash", "CUSTOMER")).thenReturn(new User(5L, "a", "hash", "CUSTOMER"));

        service.registerUser("a", "pw", "CUSTOMER", null);

        verify(userRepository, never()).createProviderProfile(any(), any());
    }

    @Test
    void registerUser_providerWithBlankDisplayName_usesUsername() {
        when(userRepository.findByUsername("tutor1")).thenReturn(null);
        when(passwordService.hashPassword("pw")).thenReturn("hash");
        when(userRepository.saveUser("tutor1", "hash", "PROVIDER")).thenReturn(new User(5L, "tutor1", "hash", "PROVIDER"));

        service.registerUser("tutor1", "pw", "PROVIDER", "  ");

        verify(userRepository).createProviderProfile(5L, "tutor1");
    }

    @Test
    void authenticate_correctPassword_returnsUser() {
        User u = new User(1L, "a", "hash", "CUSTOMER");
        when(userRepository.findByUsername("a")).thenReturn(u);
        when(passwordService.checkPassword("pw", "hash")).thenReturn(true);
        assertSame(u, service.authenticate("a", "pw"));
    }

    @Test
    void authenticate_wrongPassword_returnsNull() {
        when(userRepository.findByUsername("a")).thenReturn(new User(1L, "a", "hash", "CUSTOMER"));
        when(passwordService.checkPassword("bad", "hash")).thenReturn(false);
        assertNull(service.authenticate("a", "bad"));
    }

    @Test
    void authenticate_unknownUser_returnsNull() {
        when(userRepository.findByUsername("nobody")).thenReturn(null);
        assertNull(service.authenticate("nobody", "pw"));
    }
}