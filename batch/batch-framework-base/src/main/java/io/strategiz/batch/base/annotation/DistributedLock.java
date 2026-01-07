package io.strategiz.batch.base.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for distributed locking of batch job methods.
 *
 * When applied to a method, ensures only one instance across all servers
 * can execute the method at a time. Uses ShedLock under the hood.
 *
 * Usage:
 * <pre>
 * {@code
 * @DistributedLock(name = "market-data-backfill", lockAtLeast = 5, lockAtMost = 60, timeUnit = TimeUnit.MINUTES)
 * public void executeBackfill() {
 *     // Only one instance will execute this at a time
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

	/**
	 * Unique lock name. Should be globally unique across all jobs.
	 */
	String name();

	/**
	 * Minimum time to hold the lock. Prevents rapid re-execution if job completes quickly.
	 * Default: 1 minute
	 */
	long lockAtLeast() default 1;

	/**
	 * Maximum time to hold the lock. Prevents deadlocks if job crashes without releasing.
	 * Should be longer than expected execution time. Default: 15 minutes
	 */
	long lockAtMost() default 15;

	/**
	 * Time unit for lockAtLeast and lockAtMost. Default: MINUTES
	 */
	TimeUnit timeUnit() default TimeUnit.MINUTES;

}
