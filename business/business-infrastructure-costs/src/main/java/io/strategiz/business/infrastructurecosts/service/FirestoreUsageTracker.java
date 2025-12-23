package io.strategiz.business.infrastructurecosts.service;

import io.strategiz.data.infrastructurecosts.repository.FirestoreUsageRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AOP aspect for tracking Firestore read/write operations.
 * Intercepts repository calls and records usage metrics.
 *
 * Enable with: gcp.billing.enabled=true
 */
@Aspect
@Component
@ConditionalOnProperty(name = "gcp.billing.enabled", havingValue = "true", matchIfMissing = false)
public class FirestoreUsageTracker {

    private static final Logger log = LoggerFactory.getLogger(FirestoreUsageTracker.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FirestoreUsageRepository firestoreUsageRepository;

    // In-memory counters for batching (reduce Firestore writes)
    private final AtomicLong pendingReads = new AtomicLong(0);
    private final AtomicLong pendingWrites = new AtomicLong(0);

    public FirestoreUsageTracker(FirestoreUsageRepository firestoreUsageRepository) {
        this.firestoreUsageRepository = firestoreUsageRepository;
    }

    /**
     * Pointcut for read operations (find*, get*, load*)
     */
    @Pointcut("execution(* io.strategiz.data..repository..find*(..)) || " +
              "execution(* io.strategiz.data..repository..get*(..)) || " +
              "execution(* io.strategiz.data..repository..load*(..))")
    public void readOperations() {}

    /**
     * Pointcut for write operations (save*, update*, create*, delete*, remove*)
     */
    @Pointcut("execution(* io.strategiz.data..repository..save*(..)) || " +
              "execution(* io.strategiz.data..repository..update*(..)) || " +
              "execution(* io.strategiz.data..repository..create*(..)) || " +
              "execution(* io.strategiz.data..repository..delete*(..)) || " +
              "execution(* io.strategiz.data..repository..remove*(..))")
    public void writeOperations() {}

    /**
     * Track read operations after they complete
     */
    @AfterReturning(pointcut = "readOperations()", returning = "result")
    public void trackReadOperation(JoinPoint joinPoint, Object result) {
        try {
            String collectionName = extractCollectionName(joinPoint);
            int documentCount = countDocuments(result);

            if (documentCount > 0) {
                recordReadAsync(collectionName, documentCount);
            }
        } catch (Exception e) {
            // Don't let tracking errors affect the main operation
            log.debug("Error tracking read operation: {}", e.getMessage());
        }
    }

    /**
     * Track write operations after they complete
     */
    @AfterReturning(pointcut = "writeOperations()", returning = "result")
    public void trackWriteOperation(JoinPoint joinPoint, Object result) {
        try {
            String collectionName = extractCollectionName(joinPoint);
            int documentCount = result != null ? 1 : 0;

            if (joinPoint.getSignature().getName().startsWith("delete")) {
                recordDeleteAsync(collectionName, documentCount);
            } else {
                recordWriteAsync(collectionName, documentCount);
            }
        } catch (Exception e) {
            // Don't let tracking errors affect the main operation
            log.debug("Error tracking write operation: {}", e.getMessage());
        }
    }

    /**
     * Async recording to not block main operations
     */
    @Async
    protected void recordReadAsync(String collection, int count) {
        String today = LocalDate.now().format(DATE_FORMAT);
        firestoreUsageRepository.incrementReads(today, collection, count);
    }

    @Async
    protected void recordWriteAsync(String collection, int count) {
        String today = LocalDate.now().format(DATE_FORMAT);
        firestoreUsageRepository.incrementWrites(today, collection, count);
    }

    @Async
    protected void recordDeleteAsync(String collection, int count) {
        String today = LocalDate.now().format(DATE_FORMAT);
        // Deletes are tracked separately in the entity
        // For now, count them as writes for cost estimation purposes
        firestoreUsageRepository.incrementWrites(today, collection, count);
    }

    /**
     * Extract collection name from the repository class
     */
    private String extractCollectionName(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // Remove "Repository" suffix and convert to collection name
        String baseName = className.replace("Repository", "")
                                    .replace("Impl", "");

        // Convert camelCase to snake_case
        return camelToSnake(baseName);
    }

    /**
     * Count the number of documents in the result
     */
    private int countDocuments(Object result) {
        if (result == null) {
            return 0;
        }

        if (result instanceof Optional<?> opt) {
            return opt.isPresent() ? 1 : 0;
        }

        if (result instanceof Collection<?> coll) {
            return coll.size();
        }

        // Single document
        return 1;
    }

    private String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));

        for (int i = 1; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}
