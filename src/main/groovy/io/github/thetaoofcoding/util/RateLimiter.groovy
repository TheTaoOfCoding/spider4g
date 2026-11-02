package io.github.thetaoofcoding.util

import java.util.concurrent.Semaphore

class RateLimiter {
    public static final int PERMITS = 3
    private static final Semaphore semaphore = new Semaphore(PERMITS)

    static void acquire() {
        semaphore.acquire()
    }

    static <T, E extends Throwable> void release(T t, E e) {
        semaphore.release()
    }
}
