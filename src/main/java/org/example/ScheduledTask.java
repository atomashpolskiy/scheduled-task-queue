package org.example;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.Callable;

public class ScheduledTask {

    private final Callable task;
    private final LocalDateTime time;
    private final long seqNum;

    ScheduledTask(Callable task, LocalDateTime time, long seqNum) {
        this.task = Objects.requireNonNull(task);
        this.time = Objects.requireNonNull(time);
        this.seqNum = seqNum;
    }

    public Callable getTask() {
        return task;
    }

    public LocalDateTime getTime() {
        return time;
    }

    /**
     * @return Sequential number of the task, always greater than zero
     */
    public long getSeqNum() {
        return seqNum;
    }
}
