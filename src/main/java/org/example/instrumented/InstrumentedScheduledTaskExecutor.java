package org.example.instrumented;

import org.example.ScheduledTaskExecutor;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

public class InstrumentedScheduledTaskExecutor extends ScheduledTaskExecutor {

    private final AtomicLong scheduledCount;

    public InstrumentedScheduledTaskExecutor() {
        super();
        this.scheduledCount = new AtomicLong(0);
    }

    public InstrumentedScheduledTaskExecutor(CompletionHandler handler) {
        super(handler);
        this.scheduledCount = new AtomicLong(0);
    }

    @Override
    public void schedule(LocalDateTime time, Callable task) {
        scheduledCount.incrementAndGet();
        super.schedule(time, task);
    }

    public long getScheduledCount() {
        return scheduledCount.get();
    }
}
