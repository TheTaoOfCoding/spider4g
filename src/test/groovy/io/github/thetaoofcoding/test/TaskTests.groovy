package io.github.thetaoofcoding.test

import io.github.thetaoofcoding.component.Task
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import static org.junit.jupiter.api.Assertions.*

class TaskTests {

    @Nested
    class Example {
        @Test
        void "comment task"() {
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
    class TaskCreate {
        @Test
        void "creates task from function 'of'"() {
            def task = Task.fromSync(String.&toUpperCase)
            assertEquals("HELLO", await(task("hello")))
        }

        @Test
        void "creates task from function 'identity'"() {
            def task = Task.<String> identity()
            assertEquals("hello", await(task("hello")))
        }

        @Test
        void "creates task from function 'empty'"() {
            def task = Task.empty()
            assertNull(await(task("hello")))
        }
    }


    @Nested
    class TaskCompose {
        @Test
        void "or chains two tasks"() {
            def task1 = Task.fromSync(String.&toUpperCase)
            def task2 = Task.<String, String> fromSync { str -> str + " WORLD" }
            assertEquals("HELLO WORLD", await((task1 | task2)("hello")))
        }

        @Test
        void "rightShift chains two tasks"() {
            def task1 = Task.fromSync(String.&toUpperCase)
            def task2 = Task.<String, String> fromSync { str -> str + " WORLD" }
            assertEquals("HELLO WORLD", await((task1 >> task2)("hello")))
        }
    }

    @Nested
    class TaskRetry {
        @Test
        void "retries once after first failure"() {
            def attempts = new AtomicInteger(0)
            def task = Task.fromSync { String str ->
                if (attempts.getAndIncrement() == 0) {
                    throw new RuntimeException("first attempt fails")
                }
                str
            }.retry(1L)

            assertEquals("hello", await(task("hello")))
            assertEquals(2, attempts.get())
        }
    }

    @Nested
    class TaskParallel {
        @Test
        void "runs task for each element in parallel"() {
            def task = Task.fromSync(String.&toUpperCase)
            def parallelTask = Task.withParallel({ list -> list.collect { "**$it**".toString() } },
                    task,
                    { list -> list.collect { ">>$it<<".toString() } })
            def result = await(parallelTask("hello".toList()))
            assertEquals(['>>**H**<<', '>>**E**<<', '>>**L**<<', '>>**L**<<', '>>**O**<<'],
                    result)
        }
    }
}
