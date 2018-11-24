package org.example;

import org.example.ScheduledTaskExecutor.CompletionHandler;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.LL_Result;

import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

@JCStressTest
@Outcome(id = "1, 2", expect = Expect.ACCEPTABLE, desc = "In order execution")
@Outcome(id = "2, 1", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Out of order execution")
@State
public class SeqnumRaceCondition {

    final CopyOnWriteArrayList<Long> results;
    final ScheduledTaskExecutor executor;
    final CountDownLatch latch;

    final LocalDateTime time;

    public SeqnumRaceCondition() {
        this.results = new CopyOnWriteArrayList<>();
        this.executor = new ScheduledTaskExecutor(new CompletionHandler() {
            @Override
            public void onSuccess(ScheduledTask task) {
                results.add(task.getSeqNum());
                latch.countDown();
            }

            @Override
            public void onError(ScheduledTask task) {
                throw new AssertionError("Should not happen");
            }
        });
        this.latch = new CountDownLatch(2);
        this.time = LocalDateTime.now();
    }

    @Actor
    public void producer1() {
        executor.schedule(time, () -> null);
    }

    @Actor
    public void producer2() {
        executor.schedule(time, () -> null);
    }

    @Arbiter
    public void arbiter(LL_Result r) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        r.r1 = results.get(0);
        r.r2 = results.get(1);

        executor.shutdown();
    }
}
