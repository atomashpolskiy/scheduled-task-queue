package org.example.instrumented;

import org.example.ScheduledTask;
import org.example.ScheduledTaskExecutor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class CountingCompletionHandler implements ScheduledTaskExecutor.CompletionHandler {

    private final AtomicLong successCount, errorCount;
    private final AtomicLong minDelayNanos, maxDelayNanos;
    private final RollingAverage avgDelay;

    private volatile ScheduledTask previousCompletedTask;
    private volatile boolean enforceStrictExecutionOrder;

    public CountingCompletionHandler() {
        this.successCount = new AtomicLong(0);
        this.errorCount = new AtomicLong(0);
        this.minDelayNanos = new AtomicLong(0);
        this.maxDelayNanos = new AtomicLong(0);
        this.avgDelay = new RollingAverage();
    }

    /**
     * Suitable for single-producer scenario, in which the producer himself
     * produces tasks strictly in order of their schedule time.
     *
     * Not suitable for multiple producers scenario (as there is an inherent race condition between producers and the executor,
     * which allows for arbitrary execution orders).
     */
    public void enforceStrictExecutionOrder() {
        this.enforceStrictExecutionOrder = true;
    }

    @Override
    public void onSuccess(ScheduledTask task) {
        if (enforceStrictExecutionOrder) {
            assertExecutedInOrder(task);
        }
        previousCompletedTask = task;
        successCount.incrementAndGet();
        updateDelays(task);
    }

    @Override
    public void onError(ScheduledTask task) {
        if (enforceStrictExecutionOrder) {
            assertExecutedInOrder(task);
        }
        previousCompletedTask = task;
        errorCount.incrementAndGet();
        updateDelays(task);
    }

    private void assertExecutedInOrder(ScheduledTask task) {
        if (previousCompletedTask != null) {
            int diff = previousCompletedTask.getTime().compareTo(task.getTime());
            if (diff > 0) {
                throw new IllegalStateException("Out of order execution. Previous task scheduled at " +
                        previousCompletedTask.getTime() + ", but executed before task scheduled at " + task.getTime());
            } else if (diff == 0 && previousCompletedTask.getSeqNum() > task.getSeqNum()) {
                throw new IllegalStateException("Out of order exeuction. Both tasks scheduled at: " +
                        previousCompletedTask.getTime() + ", but previous task's seqnum is " +
                        previousCompletedTask.getSeqNum() + " and current task's seqnum is " + task.getSeqNum());
            }
        }
    }

    private void updateDelays(ScheduledTask task) {
        long delay = Duration.between(task.getTime(), LocalDateTime.now()).toNanos();

        minDelayNanos.getAndUpdate(prev -> (delay < prev) ? delay : prev);
        maxDelayNanos.getAndUpdate(prev -> (delay > prev) ? delay : prev);
        avgDelay.update(delay);
    }

    public long getSuccessCount() {
        return successCount.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    public long getMinDelayNanos() {
        return minDelayNanos.get();
    }

    public long getMaxDelayNanos() {
        return maxDelayNanos.get();
    }

    public long getAvgDelayNanos() {
        return (long) avgDelay.getValue();
    }
}

class RollingAverage {
    private double value;
    private long sampleCount;

    public synchronized void update(long newValue) {
        value = value * (sampleCount / (sampleCount + 1d)) + newValue / (sampleCount + 1d);
        sampleCount++;
    }

    public double getValue() {
        return value;
    }
}
