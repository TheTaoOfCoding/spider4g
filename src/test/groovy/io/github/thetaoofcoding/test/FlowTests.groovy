package io.github.thetaoofcoding.test

import io.github.thetaoofcoding.component.Flow
import io.github.thetaoofcoding.component.Task
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger
import static org.junit.jupiter.api.Assertions.*

class FlowTests {

    @Nested
    class FactoryFlow {
        @Test
        void "工厂方法-of"() {
            def flow = Flow.fromSync(String.&toUpperCase)
            assertEquals("HELLO", flow("hello"))
        }

        @Test
        void "工厂方法-identity"() {
            def flow = Flow.<String> identity()
            assertEquals("hello", flow("hello"))
        }

        @Test
        void "工厂方法-empty"() {
            def flow = Flow.empty()
            assertNull(flow("hello"))
        }
    }

    @Nested
    class ComposeFlow {
        @Test
        void "流程组合-or"() {
            def flow1 = Flow.fromSync(String.&toUpperCase)
            def flow2 = Flow.<String, String> fromSync { str -> str + " WORLD" }
            assertEquals("HELLO WORLD", (flow1 | flow2)("hello"))
        }

        @Test
        void "流程组合-rightShift"() {
            def flow1 = Flow.fromSync(String.&toUpperCase)
            def flow2 = Flow.<String, String> fromSync { str -> str + " WORLD" }
            assertEquals("HELLO WORLD", (flow1 >> flow2)("hello"))
        }
    }

    @Nested
    class RetryFlow {
        @Test
        void "重试流程-retry"() {
            def attempts = new AtomicInteger(0)
            def flow = Flow.fromSync { String str ->
                if (attempts.getAndIncrement() == 0) {
                    throw new RuntimeException("first attempt fails")
                }
                str
            }.retry()

            assertEquals("hello", flow("hello"))
            assertEquals(2, attempts.get())
        }

        @Test
        void "重试流程-withRetry"() {
            def attempts = new AtomicInteger(0)
            def flow = Flow.withRetry(Flow.fromSync { String str ->
                if (attempts.getAndIncrement() == 0) {
                    throw new RuntimeException("first attempt fails")
                }
                str
            })

            assertEquals("hello", flow("hello"))
            assertEquals(2, attempts.get())
        }
    }

    @Nested
    class ParallelFlow {
        @Test
        void "并行流程-withParallel"() {
            def flow = Flow.fromSync(String.&toUpperCase)
            def parallelFlow = Flow.withParallel(
                    { list -> list.collect { "**$it**".toString() } },
                    flow,
                    { list -> list.collect { ">>$it<<".toString() } }
            )
            def input = "hello".collect { it.toString() }
            def result = parallelFlow(input)

            assertEquals(
                    ['>>**H**<<', '>>**E**<<', '>>**L**<<', '>>**L**<<', '>>**O**<<'],
                    result
            )
        }
    }

    @Nested
    class SequentialFlow {
        @Test
        void "顺序流程-withSequential"() {
            def ts = (1..10).toList()
            def flow = Flow.<Integer, String> fromSync { i ->
                ">>$i<<".toString().tap { println it }
            }
            def rs = Flow.withSequential(flow)(ts)
            assertEquals(['>>1<<', '>>2<<', '>>3<<', '>>4<<', '>>5<<', '>>6<<', '>>7<<', '>>8<<', '>>9<<', '>>10<<'], rs)
        }
    }

    @Nested
    class RepeatUntilFlow {
        @Test
        void "自迭代流程-withRepeatUntil"() {
            // 初始分数
            def score = 50
            def counter = new AtomicInteger(0)
            def task = Task.<Integer, Integer> fromDelay({ previous ->
                // 随机变化分数
                def change = ThreadLocalRandom.current().nextInt(-10, 11)
                println("times: ${counter.incrementAndGet()}, previous: $previous, change: $change")
                previous += change
            }, 1L)
            def flow = { -> task }

            // 重复执行任务，直到分数小于0或大于等于100 或者重复执行了30次
            def repeatUntilFlow = Flow.withRepeatUntil(flow, { it < 0 || it >= 100 || counter.get() >= 30 })
            repeatUntilFlow(score)
        }
    }
}
