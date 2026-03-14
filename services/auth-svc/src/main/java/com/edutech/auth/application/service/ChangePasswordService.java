package com.edutech.auth.application.service;

import com.edutech.auth.application.exception.IncorrectCurrentPasswordException;
import com.edutech.auth.application.exception.UserNotFoundException;
import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.in.ChangePasswordUseCase;
import com.edutech.auth.domain.port.out.PasswordHasher;
import com.edutech.auth.domain.port.out.TokenStore;
import com.edutech.auth.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ChangePasswordService implements ChangePasswordUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordService.class);

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenStore tokenStore;

    public ChangePasswordService(UserRepository userRepository,
                                 PasswordHasher passwordHasher,
                                 TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenStore = tokenStore;
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (!passwordHasher.verify(currentPassword, user.getPasswordHash())) {
            throw new IncorrectCurrentPasswordException();
        }

        String newHash = passwordHasher.hash(newPassword);
        user.updatePassword(newHash);
        userRepository.save(user);

        // Invalidate all existing sessions so other devices are logged out
        tokenStore.deleteAllForUser(userId);
        log.info("Password changed for userId={} — all sessions revoked", userId);
    }
}
