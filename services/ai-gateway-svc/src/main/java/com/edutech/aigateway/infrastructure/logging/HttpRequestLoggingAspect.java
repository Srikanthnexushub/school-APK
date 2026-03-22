package com.edutech.aigateway.infrastructure.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AOP aspect for reactive @RestController methods (ai-gateway-svc uses WebFlux).
 * Hooks into Mono/Flux reactive pipelines to log entry, exit, and errors.
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
        Object result = pjp.proceed();
        if (result instanceof Mono<?> mono) {
            return mono
                    .doOnSuccess(r -> log.info("← {} duration={}ms", signature,
                            System.currentTimeMillis() - start))
                    .doOnError(t -> log.error("✗ {} failed duration={}ms error={}", signature,
                            System.currentTimeMillis() - start, t.getMessage()));
        }
        if (result instanceof Flux<?> flux) {
            return flux
                    .doOnComplete(() -> log.info("← {} duration={}ms", signature,
                            System.currentTimeMillis() - start))
                    .doOnError(t -> log.error("✗ {} failed duration={}ms error={}", signature,
                            System.currentTimeMillis() - start, t.getMessage()));
        }
        log.info("← {} duration={}ms", signature, System.currentTimeMillis() - start);
        return result;
    }
}
