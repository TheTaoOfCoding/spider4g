package io.github.thetaoofcoding.component

import java.util.function.Function

interface Flow<T, R> {

    /*
     * 任务头结点
     */
    Task<T, R> head();

    /*
     * 启动此任务
     */
    default R call(T t) {
        await head()(t)
    }

    /*
     * 流程操作符重载 |
     */
    default <V> Flow<T, V> or(Flow<? super R, V> next) {
        { -> (head() | (next.head())) }
    }

    /*
     * 流程操作符重载 >>
     */
    default <V> Flow<T, V> rightShift(Flow<? super R, V> next) {
        { -> head() >> (next.head()) }
    }

    /*
     * 重试流程
     */
    default Flow<T, R> retry() {
        head().&retry
    }

    /*
     * 工厂方法
     */
    static <T, R> Flow<T, R> fromSync(Function<T, R> function) {
        { -> Task.fromSync(function) }
    }

    /*
     * 一致性流程
     */
    static <T> Flow<T, T> identity() {
        Task.&identity
    }

    /*
     * 空流程
     */
    static <T, R> Flow<T, R> empty() {
        Task.&empty
    }

    /*
     * 重试指定流程
     */
    static <T, R> Flow<T, R> withRetry(Flow<T, R> flow) {
        flow.retry()
    }

    /*
     * 并行流程
     */
    static <T, R> Flow<List<T>, List<R>> withParallel(Function<List<T>, List<T>> before, Flow<? super T, R> flow, Function<List<R>, List<R>> after) {
        { -> Task.withParallel(before, flow.head(), after) }
    }
}