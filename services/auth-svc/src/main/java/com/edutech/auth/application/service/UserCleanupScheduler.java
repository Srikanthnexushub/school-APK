// src/main/java/com/edutech/auth/application/service/UserCleanupScheduler.java
package com.edutech.auth.application.service;

import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.out.UserRepository;
import com.edutech.auth.application.config.CleanupProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class UserCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(UserCleanupScheduler.class);

    private final UserRepository userRepository;
    private final CleanupProperties props;

    public UserCleanupScheduler(UserRepository userRepository, CleanupProperties props) {
        this.userRepository = userRepository;
        this.props = props;
    }

    @Scheduled(cron = "${auth.cleanup.cron}")
    @Transactional
    public void purgeExpiredUnverifiedAccounts() {
        Instant cutoff = Instant.now().minus(props.retentionHours(), ChronoUnit.HOURS);
        List<User> expired = userRepository.findExpiredPendingVerification(cutoff);
        if (expired.isEmpty()) {
            log.debug("No expired unverified accounts to purge.");
            return;
        }
        for (User user : expired) {
            user.deactivate();
            userRepository.save(user);
        }
        log.info("Purged {} expired unverified account(s) older than {} hours.", expired.size(), props.retentionHours());
    }
}
