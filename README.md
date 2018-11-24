# scheduled-task-queue

Однопоточный планировщик задач на основе неограниченной по размеру очереди.

### Использование

```java
ScheduledTaskExecutor executor = new ScheduledTaskExecutor();
executor.schedule(LocalDateTime.now(), () -> null);
...
executor.shutdown();
```

### Свойства

- задачи выполняются в порядке, определяемом параметром LocalDateTime (т.е. задачи с меньшим LocalDateTime выполняются раньше, чем задачи с большим LocalDateTime)
- задачи выполняются настолько быстро, насколько это возможно при текущей нагрузке (т.е. задача может быть выполнена позже указанного LocalDateTime)
- задачи со значением LocalDateTime в прошлом выполняются раньше, чем задачи со значением LocalDateTime в настоящем и будущем
- задачи с одинаковым значением LocalDateTime выполняются в порядке коммита задач в очередь (см. ниже)
- метод ScheduledTaskExecutor.schedule никогда не блокирует вызывающий поток

### Выполнение задач с одинаковым LocalDateTime

Планировщик гарантирует выполнение задач с одинаковым LocalDateTime в соответствии с порядком добавления задач в очередь. Это обеспечивается тем, что присвоение задаче порядкового номера и добавление в очередь выполняется как атомарная операция:

```java
public class ScheduledTaskQueue {

    private final DelayQueue<DelayedTask> queue;
    private long counter;

    public synchronized void add(LocalDateTime time, Callable task) {
        queue.put(new DelayedTask(task, time, ++counter));
    }
    ...
}
```

К сожалению, использование исключительного доступа подразумевает возможность соревнования между вызывающими метод schedule потоками, что отрицательно сказывается на "пропускной способности" планировщика (имеется в виду количество добавленных в очередь задач).

Решить эту проблему можно только отказавшись от требования выполнения задач с одинаковым LocalDateTime в строгом соответствии с порядком добавления в очередь. Тогда можно будет использовать иную версию метода `ScheduledTaskQueue.add`:

```java
public class ScheduledTaskQueue {

    private final DelayQueue<DelayedTask> queue;
    private final AtomicLong atomicCounter = new AtomicLong(0);

    public void add(LocalDateTime time, Callable task) {
        queue.put(new DelayedTask(task, time, atomicCounter.incrementAndGet()));
    }
    ...
}
```

Сравнить производительность обоих решений можно запустив демонстрационный класс `org.example.Main`.

```
# Вывод программы, использующей синхронизированный метод ScheduledTaskQueue.add

# 1) Один поток добавляет задачи в очередь
scheduled: 520 240, success: 423 325, error: 0, min delay: 0 ms, max delay: 202 ms, avg delay: 34
scheduled: 991 490, success: 991 475, error: 0, min delay: 0 ms, max delay: 302 ms, avg delay: 49
scheduled: 1 595 331, success: 1 595 330, error: 0, min delay: 0 ms, max delay: 302 ms, avg delay: 30
scheduled: 2 273 464, success: 2 273 463, error: 0, min delay: 0 ms, max delay: 302 ms, avg delay: 21
scheduled: 2 945 873, success: 2 945 870, error: 0, min delay: 0 ms, max delay: 302 ms, avg delay: 16
scheduled: 3 619 005, success: 3 618 917, error: 0, min delay: 0 ms, max delay: 302 ms, avg delay: 13
scheduled: 4 281 637, success: 4 281 638, error: 0, min delay: 0 ms, max delay: 302 ms, avg delay: 11

# 2) Два потока добавляют задачи в очередь
scheduled: 358 254, success: 358 276, error: 0, min delay: 0 ms, max delay: 39 ms, avg delay: 5
scheduled: 725 370, success: 725 368, error: 0, min delay: 0 ms, max delay: 39 ms, avg delay: 3
scheduled: 1 011 017, success: 1 011 012, error: 0, min delay: 0 ms, max delay: 39 ms, avg delay: 2
scheduled: 1 350 101, success: 1 350 099, error: 0, min delay: 0 ms, max delay: 39 ms, avg delay: 1
scheduled: 1 672 500, success: 1 672 498, error: 0, min delay: 0 ms, max delay: 39 ms, avg delay: 1
scheduled: 1 980 076, success: 1 980 075, error: 0, min delay: 0 ms, max delay: 39 ms, avg delay: 1
scheduled: 2 290 023, success: 2 290 021, error: 0, min delay: 0 ms, max delay: 39 ms, avg delay: 1

# 3) Три потока добавляют задачи в очередь
scheduled: 297 468, success: 297 474, error: 0, min delay: 0 ms, max delay: 17 ms, avg delay: 0
scheduled: 632 192, success: 632 189, error: 0, min delay: 0 ms, max delay: 17 ms, avg delay: 0
scheduled: 938 332, success: 938 329, error: 0, min delay: 0 ms, max delay: 17 ms, avg delay: 0
scheduled: 1 223 202, success: 1 223 199, error: 0, min delay: 0 ms, max delay: 17 ms, avg delay: 0
scheduled: 1 525 790, success: 1 525 787, error: 0, min delay: 0 ms, max delay: 17 ms, avg delay: 0
scheduled: 1 807 342, success: 1 807 339, error: 0, min delay: 0 ms, max delay: 17 ms, avg delay: 0
scheduled: 2 101 058, success: 2 101 055, error: 0, min delay: 0 ms, max delay: 17 ms, avg delay: 0
```

```
# Вывод программы, использующей несинхронизированный метод ScheduledTaskQueue.add

# 1) Один поток добавляет задачи в очередь
scheduled: 588 760, success: 588 766, error: 0, min delay: 0 ms, max delay: 23 ms, avg delay: 2
scheduled: 1 297 239, success: 1 297 238, error: 0, min delay: 0 ms, max delay: 23 ms, avg delay: 1
scheduled: 2 020 285, success: 2 020 226, error: 0, min delay: 0 ms, max delay: 23 ms, avg delay: 0
scheduled: 2 737 589, success: 2 737 549, error: 0, min delay: 0 ms, max delay: 23 ms, avg delay: 0
scheduled: 3 466 504, success: 3 466 495, error: 0, min delay: 0 ms, max delay: 23 ms, avg delay: 0
scheduled: 4 171 995, success: 4 171 972, error: 0, min delay: 0 ms, max delay: 23 ms, avg delay: 0
scheduled: 4 883 711, success: 4 883 710, error: 0, min delay: 0 ms, max delay: 23 ms, avg delay: 0

# 2) Два потока добавляют задачи в очередь
scheduled: 621 508, success: 241 827, error: 0, min delay: 0 ms, max delay: 458 ms, avg delay: 216
scheduled: 1 241 889, success: 537 750, error: 0, min delay: 0 ms, max delay: 1 139 ms, avg delay: 529
scheduled: 1 865 004, success: 831 759, error: 0, min delay: 0 ms, max delay: 1 648 ms, avg delay: 834
scheduled: 2 174 645, success: 962 996, error: 0, min delay: 0 ms, max delay: 2 464 ms, avg delay: 1 018
scheduled: 2 805 290, success: 1 232 004, error: 0, min delay: 0 ms, max delay: 3 021 ms, avg delay: 1 390
scheduled: 3 473 449, success: 1 516 567, error: 0, min delay: 0 ms, max delay: 3 596 ms, avg delay: 1 750
scheduled: 4 004 226, success: 1 755 509, error: 0, min delay: 0 ms, max delay: 4 192 ms, avg delay: 2 040

# 3) Три потока добавляют задачи в очередь
scheduled: 787 668, success: 210 729, error: 0, min delay: 0 ms, max delay: 580 ms, avg delay: 287
scheduled: 1 330 549, success: 384 823, error: 0, min delay: 0 ms, max delay: 1 123 ms, avg delay: 551
scheduled: 2 106 567, success: 656 118, error: 0, min delay: 0 ms, max delay: 2 344 ms, avg delay: 1 119
scheduled: 2 375 813, success: 749 043, error: 0, min delay: 0 ms, max delay: 3 242 ms, avg delay: 1 345
scheduled: 3 113 001, success: 1 000 792, error: 0, min delay: 0 ms, max delay: 3 883 ms, avg delay: 1 894
scheduled: 3 892 750, success: 1 272 453, error: 0, min delay: 0 ms, max delay: 4 560 ms, avg delay: 2 391
scheduled: 4 544 624, success: 1 499 474, error: 0, min delay: 0 ms, max delay: 4 797 ms, avg delay: 2 725
```

Из приведенных выше цифр видно, что в случае с несинхронизированной версией `add` пропускная способность остается практически неизменной при увеличении количества потоков, добавляющих новые задачи в очередь (следует отметить, что скорость обработки задач планировщиком остается неизменной, т.к. используется один поток, и выполнение задач начинает отставать от графика, т.е. система оказывается перегруженной.)

### Доказательство наличия race condition в несинхронизированной версии метода ScheduledTaskQueue.add

Чтобы продемонстрировать нарушение условия о строгом порядке выполнения задач, можно воспользоваться инструментом `jcstress`. Содержимое тестового кейса выглядит следующим образом:

```java
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
```

Запустить тест можно выполнив команду `./gradlew clean build -x test jcstress`.

Результаты при использовании синхронизированного метода `add`:

```
RUN RESULTS:
------------------------------------------------------------------------------------------------------------------------

*** INTERESTING tests
  Some interesting behaviors observed. This is for the plain curiosity.

  0 matching test results. 

*** FAILED tests
  Strong asserts were violated. Correct implementations should have no assert failures here.

  0 matching test results. 

*** ERROR tests
  Tests break for some reason, other than failing the assert. Correct implementations should have none.

  0 matching test results. 

*** All remaining tests
  Tests that do not fall into any of the previous categories.

  4 matching test results.  Use -v to print them.
```

Результаты при использовании несинхронизированного метода `add`:

```
RUN RESULTS:
------------------------------------------------------------------------------------------------------------------------

*** INTERESTING tests
  Some interesting behaviors observed. This is for the plain curiosity.

  1 matching test results. 
      [OK] org.example.SeqnumRaceCondition
    (JVM args: [-XX:+UnlockDiagnosticVMOptions, -XX:+WhiteBoxAPI, -XX:-RestrictContended, -Dfile.encoding=UTF-8, -Duser.country=US, -Duser.language=en, -Duser.variant, -Xint])
  Observed state   Occurrences              Expectation  Interpretation                                              
            1, 2        49,120               ACCEPTABLE  In order execution                                          
            2, 1        32,768   ACCEPTABLE_INTERESTING  Out of order execution                                      


*** FAILED tests
  Strong asserts were violated. Correct implementations should have no assert failures here.

  0 matching test results. 

*** ERROR tests
  Tests break for some reason, other than failing the assert. Correct implementations should have none.

  0 matching test results. 

*** All remaining tests
  Tests that do not fall into any of the previous categories.

  3 matching test results.  Use -v to print them.      
```
