package io.github.thetaoofcoding.component

import groovy.concurrent.Awaitable
import java.util.concurrent.TimeUnit
import java.util.function.Function

interface Task<T, R> extends Function<T, Awaitable<R>> {

    Awaitable<R> execute(T t);

    default Awaitable<R> apply(T t) {
        execute(t)
    }

    default Awaitable<R> call(T t) {
        execute(t)
    }

    /*
     * 任务操作符重载 |
     */
    default <V> Task<T, V> or(Task<? super R, V> next) {
        { t -> execute(t).thenCompose next }
    }

    /*
     * 任务操作符重载 >>
     */
    default <V> Task<T, V> rightShift(Task<? super R, V> next) {
        { t -> execute(t).thenCompose next }
    }

    /*
     * 重试任务
     */
    default Task<T, R> retry() {
        retry(1L, 2L)
    }

    /*
     * 重试任务
     */
    default Task<T, R> retry(long delay) {
        { t -> execute(t).exceptionally { await Awaitable.delay(delay, TimeUnit.SECONDS).thenCompose { execute(t) } } }
    }

    /*
     * 重试任务
     */
    default Task<T, R> retry(long delay, long times) {
        if (times == 1) return retry(delay)
        retry(delay, times - 1).retry(delay + 1)
    }

    /*
     * 工厂方法
     */
    static <T, R> Task<T, R> fromSync(Function<T, R> function) {
        { t -> async { function.apply(t) } }
    }

    /*
     * 一致性任务
     */
    static <T> Task<T, T> identity() {
        { t -> Awaitable.<T>of(t) }
    }

    /*
     * 空任务
     */
    static <T, R> Task<T, R> empty() {
        { _ -> Awaitable.<R>of(null) }
    }

    static <T, R> Task<List<T>, List<R>> withParallel(Function<List<T>, List<T>> before, Task<? super T, R> task) {
        Task.withParallel(before, task, { it })
    }

    static <T, R> Task<List<T>, List<R>> withParallel(Task<? super T, R> task) {
        Task.withParallel({ it } as Function<List<T>, List<T>>, task, { it })
    }

    static <T, R> Task<List<T>, List<R>> withParallel(Task<? super T, R> task, Function<List<R>, List<R>> after) {
        Task.withParallel({ it } as Function<List<T>, List<T>>, task, after)
    }

    /*
     * 并行任务
     */
    static <T, R> Task<List<T>, List<R>> withParallel(Function<List<T>, List<T>> before, Task<? super T, R> task, Function<List<R>, List<R>> after) {
        {
            list -> Awaitable.<List<T>>of(list)
                        .then(before)
                        .then { it.collect(task.&execute) as Awaitable[] }
                        .thenCompose { array -> Awaitable.all(array) }
                        .then(after)
        }
    }
}