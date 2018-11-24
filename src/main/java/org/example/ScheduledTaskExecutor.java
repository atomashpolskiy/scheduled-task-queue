package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Callable;

public class ScheduledTaskExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskExecutor.class);

    private final ScheduledTaskQueue queue;
    private final Thread executor;

    public ScheduledTaskExecutor() {
        this(CompletionHandler.NOOP_HANDLER);
    }

    public ScheduledTaskExecutor(CompletionHandler handler) {
        Objects.requireNonNull(handler);

        this.queue = new ScheduledTaskQueue();

        Thread executor = new Thread(() -> {
            ScheduledTask task;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    task = queue.take();
                    try {
                        task.getTask().call();
                        handler.onSuccess(task);
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        } else {
                            LOGGER.error("Failed to execute task", e);
                            handler.onError(task);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        },"scheduled-task-executor");
        executor.setUncaughtExceptionHandler((t, e) -> {
            LOGGER.error("Uncaught error", e);
        });
        executor.start();
        this.executor = executor;
    }

    public void schedule(LocalDateTime time, Callable task) {
        queue.add(time, task);
    }

    public void shutdown() {
        executor.interrupt();
        // cleanup to enable garbage collection of obsolete tasks
        queue.clear();
    }

    public interface CompletionHandler {
        void onSuccess(ScheduledTask task);
        void onError(ScheduledTask task);

        CompletionHandler NOOP_HANDLER = new CompletionHandler() {
            @Override
            public void onSuccess(ScheduledTask task) {
                // do nothing
            }

            @Override
            public void onError(ScheduledTask task) {
                // do nothing
            }
        };
    }
}
