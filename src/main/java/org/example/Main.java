package org.example;

import org.example.instrumented.CountingCompletionHandler;
import org.example.instrumented.InstrumentedScheduledTaskExecutor;

import java.time.Duration;
import java.time.LocalDateTime;

public class Main {

    private static final long WATCH_PERIOD_MILLIS = 1000;

    public static void main(String[] args) {
        CountingCompletionHandler completionHandler = new CountingCompletionHandler();
        InstrumentedScheduledTaskExecutor executor = new InstrumentedScheduledTaskExecutor(completionHandler);

        Thread watch = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(WATCH_PERIOD_MILLIS);
                    System.err.println(String.format(
                            "scheduled: %,d, success: %,d, error: %,d, min delay: %,d ms, max delay: %,d ms, avg delay: %,d",
                            executor.getScheduledCount(),
                            completionHandler.getSuccessCount(),
                            completionHandler.getErrorCount(),
                            Duration.ofNanos(completionHandler.getMinDelayNanos()).toMillis(),
                            Duration.ofNanos(completionHandler.getMaxDelayNanos()).toMillis(),
                            Duration.ofNanos(completionHandler.getAvgDelayNanos()).toMillis()));
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        watch.setDaemon(true);
        watch.start();

        Thread producer1 = new Thread(() -> produce(executor), "producer-1");
        Thread producer2 = new Thread(() -> produce(executor), "producer-1");

        producer1.start();
        producer2.start();
    }

    private static void produce(ScheduledTaskExecutor executor) {
        while (true) {
            executor.schedule(LocalDateTime.now(), () -> null);
        }
    }
}
