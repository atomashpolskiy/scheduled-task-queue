package org.example;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ScheduledTaskQueue {

    private final DelayQueue<DelayedTask> queue;
    private long counter;

    public ScheduledTaskQueue() {
        this.queue = new DelayQueue<>();
    }

    public synchronized void add(LocalDateTime time, Callable task) {
        queue.put(new DelayedTask(task, time, ++counter));
    }

    // ** NOTE: This version of 'add' does not use locking and, subsequently, has much better throughput.
    // Unfortunately, it creates a race condition between producers regarding which producer
    // is first to put his task in the queue. Hence, given two tasks with equal schedule time but different
    // seqnums, the task with greater seqnum may be executed before the task with lesser seqnum. **
//    private final AtomicLong atomicCounter = new AtomicLong(0);
//    public void add(LocalDateTime time, Callable task) {
//        queue.put(new DelayedTask(task, time, atomicCounter.incrementAndGet()));
//    }

    public ScheduledTask take() throws InterruptedException {
        return queue.take();
    }

    public void clear() {
        queue.clear();
    }

    private static class DelayedTask extends ScheduledTask implements Delayed {

        DelayedTask(Callable task, LocalDateTime time, long seqNum) {
            super(task, time, seqNum);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return LocalDateTime.now().until(getTime(), ChronoUtil.toChronoUnit(unit));
        }

        @Override
        public int compareTo(Delayed o) {
            DelayedTask that = (DelayedTask) o;
            return TaskComparator.INSTANCE.compare(this, that);
        }
    }

    private static class TaskComparator implements Comparator<ScheduledTask> {
        static final TaskComparator INSTANCE = new TaskComparator();

        @Override
        public int compare(ScheduledTask task1, ScheduledTask task2) {
            LocalDateTime d1 = task1.getTime(),
                          d2 = task2.getTime();

            int ret = d1.compareTo(d2);
            if (ret == 0) {
                // break tie by seqNum; it's safe to use subtraction, because seqNum is always positive
                return (int) (task1.getSeqNum() - task2.getSeqNum());
            } else {
                return ret;
            }
        }
    }
}
