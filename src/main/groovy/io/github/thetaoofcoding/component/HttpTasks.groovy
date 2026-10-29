package io.github.thetaoofcoding.component

import groovy.concurrent.Awaitable
import groovy.http.HttpBuilder
import groovy.http.HttpResult
import groovy.util.logging.Slf4j
import java.util.concurrent.CompletableFuture

@Slf4j
class HttpTasks {

    static class HttpArgs {
        String httpUri = ""
        Map<String, ?> httpHeaders = [:]
        Map<String, String> httpQueries = [:]
    }

    static Task<Closure, HttpResult> httpGet() {
        { @DelegatesTo(value = HttpArgs, strategy = Closure.DELEGATE_FIRST) Closure closure ->
            def httpArgs = new HttpArgs()
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure.delegate = httpArgs
            closure()
            def (httpUri, httpHeaders, httpQueries) = [httpArgs.httpUri, httpArgs.httpHeaders, httpArgs.httpQueries]
            def r = HttpBuilder.http {}.getAsync(httpUri) {
                headers httpHeaders
                query httpQueries
            } as CompletableFuture<HttpResult>
            Awaitable.from(r)
        }
    }
}