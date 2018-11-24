package org.example;

import org.example.instrumented.CountingCompletionHandler;
import org.example.instrumented.InstrumentedScheduledTaskExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScheduledTaskExecutorTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskExecutorTest.class);

    private volatile CountingCompletionHandler completionHandler;
    private volatile InstrumentedScheduledTaskExecutor executor;

    @Before
    public void before() {
        completionHandler = new CountingCompletionHandler();
        executor = new InstrumentedScheduledTaskExecutor(completionHandler);
    }

    @After
    public void after() {
        executor.shutdown();
    }

    @Test
    public void test_SingleProducer() {
        doTest(createProducerFactory(new OrderedTimeSupplier()),1, Duration.ofSeconds(10));
    }

    @Test
    public void test_SingleProducer_Seqnum() {
        LocalDateTime time = LocalDateTime.now();
        Supplier<Thread> constantTimeProducer = createProducerFactory(() -> time);
        doTest(constantTimeProducer,1, Duration.ofSeconds(10));
    }

    @Test
    public void test_TwoProducers() {
        doTest(createProducerFactory(new OrderedTimeSupplier()),2, Duration.ofSeconds(10));
    }

    @Test
    public void test_ThreeProducers() {
        doTest(createProducerFactory(new OrderedTimeSupplier()),3, Duration.ofSeconds(10));
    }

    private static class OrderedTimeSupplier implements Supplier<LocalDateTime> {
        private LocalDateTime time = LocalDateTime.now();

        @Override
        public LocalDateTime get() {
            LocalDateTime ret = time;
            time = time.plusNanos(100_000); // 0.1 ms
            return ret;
        }
    }

    private Supplier<Thread> createProducerFactory(Supplier<LocalDateTime> timeSupplier) {
        AtomicLong counter = new AtomicLong(1);
        return () -> new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                executor.schedule(timeSupplier.get(), () -> null);
            }
        }, "producer-" + counter.getAndIncrement());
    }

    private void doTest(Supplier<Thread> producerFactory, int producersCount, Duration testDuration) {
        List<Thread> producers = new ArrayList<>(producersCount + 1);
        while (producersCount-- > 0) {
            producers.add(producerFactory.get());
        }
        doTest(producers, testDuration);
    }

    private void doTest(Collection<Thread> producers, Duration testDuration) {
        if (producers.size() == 1) {
            completionHandler.enforceStrictExecutionOrder();
        }
        producers.forEach(Thread::start);
        try {
            Thread.sleep(testDuration.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpectedly interrupted", e);
        }
        producers.forEach(Thread::interrupt);

        LOGGER.info(String.format(
                "#of producers: %d, scheduled: %,d, success: %,d, error: %,d, min delay: %,d ms, max delay: %,d ms, avg delay: %d ms",
                producers.size(),
                executor.getScheduledCount(),
                completionHandler.getSuccessCount(),
                completionHandler.getErrorCount(),
                Duration.ofNanos(completionHandler.getMinDelayNanos()).toMillis(),
                Duration.ofNanos(completionHandler.getMaxDelayNanos()).toMillis(),
                Duration.ofNanos(completionHandler.getAvgDelayNanos()).toMillis()));
    }

    @Test
    public void test_SingleProducer_ScheduleInFuture() {
        Thread producer = createProducerFactory(() -> LocalDateTime.now().plusMinutes(1)).get();
        producer.start();
        try {
            Thread.sleep(Duration.ofSeconds(5).toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpectedly interrupted", e);
        }
        producer.interrupt();

        assertTrue(executor.getScheduledCount() > 0);
        assertEquals(0, completionHandler.getSuccessCount());
        assertEquals(0, completionHandler.getErrorCount());
    }
}