package io.github.thetaoofcoding.test

import groovy.concurrent.Awaitable
import io.github.thetaoofcoding.component.Task
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import static org.junit.jupiter.api.Assertions.*

class TaskTests {

    @Nested
    class Example {
        @Test
        void "任务组合示例"() {
            def commentTask = Task.<String, String> fromSync { it.toUpperCase() }
                    | { str -> async { " $str " } }
                    | { str -> async { "-$str-" } }
                    | { str -> async { "-$str-" } }
                    | { str -> async { "!$str" } }
                    | { str -> async { "<$str>".toString() } }
            def result = await commentTask("hello")
            assertEquals("<!-- HELLO -->", result)
        }
    }

    @Nested
    class FactoryTask {
        @Test
        void "工厂方法-of"() {
            def task = Task.fromSync(String.&toUpperCase)
            assertEquals("HELLO", await(task("hello")))
        }

        @Test
        void "工厂方法-identity"() {
            def task = Task.<String> identity()
            assertEquals("hello", await(task("hello")))
        }

        @Test
        void "工厂方法-empty"() {
            def task = Task.empty()
            assertNull(await(task("hello")))
        }
    }

    @Nested
    class ComposeTask {
        @Test
        void "任务组合-or"() {
            def task1 = Task.fromSync(String.&toUpperCase)
            def task2 = Task.<String, String> fromSync { str -> str + " WORLD" }
            assertEquals("HELLO WORLD", await((task1 | task2)("hello")))
        }

        @Test
        void "任务组合-rightShift"() {
            def task1 = Task.fromSync(String.&toUpperCase)
            def task2 = Task.<String, String> fromSync { str -> str + " WORLD" }
            assertEquals("HELLO WORLD", await((task1 >> task2)("hello")))
        }
    }

    @Nested
    class RetryTask {
        @Test
        void "重试任务-retry"() {
            def attempts = new AtomicInteger(0)
            def task = Task.fromSync { String str ->
                if (attempts.getAndIncrement() == 0) {
                    throw new RuntimeException("first attempt fails")
                }
                str
            }.retry(1L)

            assertEquals("hello", await(task("hello")))
            // 方法调用次数：第一次调用失败，第二次调用成功
            assertEquals(2, attempts.get())
        }
    }

    @Nested
    class DelayTask {
        @Test
        void "延迟任务-delay"() {
            def delayedTask = Task.fromDelay({ String str -> ">>${str.toUpperCase()}<<".toString() }, 1)
            long start = System.nanoTime()
            def result = await delayedTask("hello")
            long end = System.nanoTime()
            def duration = (end - start) / 1_000_000_000
            assertEquals('>>HELLO<<', result)
            assertEquals(duration.intValue(), 1)
        }
    }

    @Nested
    class ParallelTask {
        @Test
        void "并行任务-parallel"() {
            def delayedTask = Task.fromDelay({ String str -> ">>${str.toUpperCase()}<<".toString() }, 1)
            def paralleledTask = Task.withParallel(delayedTask)
            long start = System.nanoTime()
            def result = await paralleledTask("hello".toList())
            long end = System.nanoTime()
            def duration = (end - start) / 1_000_000_000
            assertEquals(['>>H<<', '>>E<<', '>>L<<', '>>L<<', '>>O<<'], result)
            assertEquals(duration.intValue(), 1)
        }
    }

    @Nested
    class RateLimitTask {
        @Test
        void "限流任务-rateLimit"() {
            def delay = 1L
            def syncedTask = Task.fromSync { it }
            def rateLimitedTask = Task.withRateLimit(syncedTask, delay)
            def paralleledTask = Task.withParallel(rateLimitedTask)
            def list = (1..20).toList()
            long start = System.nanoTime()
            await paralleledTask(list)
            long end = System.nanoTime()
            def duration = (end - start) / 1_000_000_000
            // 20 / (RateLimiter.PERMITS * delay) = 4
            assertEquals(duration.intValue(), 4)
        }
    }
}
