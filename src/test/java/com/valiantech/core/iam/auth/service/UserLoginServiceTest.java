package com.valiantech.core.iam.auth.service;

import com.valiantech.core.iam.auth.model.UserLogin;
import com.valiantech.core.iam.auth.repository.UserLoginRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("UserLoginService Tests")
@ExtendWith(MockitoExtension.class)
class UserLoginServiceTest {

    @Mock
    private UserLoginRepository userLoginRepository;

    @InjectMocks
    private UserLoginService userLoginService;

    @Nested
    @DisplayName("recordLoginAttempt Tests")
    class RecordLoginAttemptTests {

        @Test
        @DisplayName("Should save login attempt successfully when data is valid")
        void shouldSaveLoginAttemptSuccessfully() {
            // Arrange
            UUID userId = UUID.randomUUID();
            UUID companyId = UUID.randomUUID();
            String ipAddress = "192.168.1.1";
            String userAgent = "JUnitTestAgent";
            boolean success = true;
            String failureReason = null;

            when(userLoginRepository.save(any(UserLogin.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            userLoginService.recordLoginAttempt(userId, companyId, ipAddress, userAgent, success, failureReason);

            // Assert
            ArgumentCaptor<UserLogin> captor = ArgumentCaptor.forClass(UserLogin.class);
            verify(userLoginRepository, times(1)).save(captor.capture());

            UserLogin savedLogin = captor.getValue();
            Assertions.assertEquals(userId, savedLogin.getUserId());
            Assertions.assertEquals(companyId, savedLogin.getCompanyId());
            Assertions.assertEquals(ipAddress, savedLogin.getIpAddress());
            Assertions.assertEquals(userAgent, savedLogin.getUserAgent());
            Assertions.assertEquals(success, savedLogin.isSuccess());
            Assertions.assertEquals(failureReason, savedLogin.getFailureReason());
            Assertions.assertNotNull(savedLogin.getLoginAt());
            Assertions.assertNotNull(savedLogin.getId());
        }
    }
}
