package io.github.thetaoofcoding.test

import io.github.thetaoofcoding.component.Flow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import static org.junit.jupiter.api.Assertions.*

class FlowTests {

    @Nested
    class FlowCreate {
        @Test
        void "creates flow from function 'of'"() {
            def flow = Flow.fromSync(String.&toUpperCase)
            assertEquals("HELLO", flow("hello"))
        }

        @Test
        void "creates flow from 'identity'"() {
            def flow = Flow.<String> identity()
            assertEquals("hello", flow("hello"))
        }

        @Test
        void "creates flow from 'empty'"() {
            def flow = Flow.empty()
            assertNull(flow("hello"))
        }
    }

    @Nested
    class FlowCompose {
        @Test
        void "or chains two flows"() {
            def flow1 = Flow.fromSync(String.&toUpperCase)
            def flow2 = Flow.<String, String> fromSync { str -> str + " WORLD" }
            assertEquals("HELLO WORLD", (flow1 | flow2)("hello"))
        }

        @Test
        void "rightShift chains two flows"() {
            def flow1 = Flow.fromSync(String.&toUpperCase)
            def flow2 = Flow.<String, String> fromSync { str -> str + " WORLD" }
            assertEquals("HELLO WORLD", (flow1 >> flow2)("hello"))
        }
    }

    @Nested
    class FlowRetry {
        @Test
        void "retries once after first failure"() {
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
        void "withRetry delegates to retry"() {
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
    class FlowParallel {
        @Test
        void "runs flow for each element in parallel"() {
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
}
