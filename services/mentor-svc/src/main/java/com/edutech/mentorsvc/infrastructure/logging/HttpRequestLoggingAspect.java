package com.edutech.mentorsvc.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that logs every @RestController method invocation:
 * entry, exit with duration, and errors.
 * Keeps controllers thin while providing full HTTP request observability.
 */
@Aspect
@Component
public class HttpRequestLoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingAspect.class);


    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        String signature = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        log.info("→ {}", signature);
        try {
            Object result = pjp.proceed();
            log.info("← {} duration={}ms", signature, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable t) {
            log.error("✗ {} failed duration={}ms error={}", signature,
                    System.currentTimeMillis() - start, t.getMessage());
            throw t;
        }
    }
}
