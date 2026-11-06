package io.github.thetaoofcoding.factory

import groovy.concurrent.Awaitable
import groovy.http.HttpResult
import io.github.thetaoofcoding.component.Task
import org.jsoup.nodes.Document
import java.util.function.Function

class DocTasks {

    static <R> Task<HttpResult, R> body(Function<String, R> fn) {
        { httpResult -> Awaitable.of(httpResult.body()).then(fn) }
    }

    static <R> Task<HttpResult, R> html(Function<Document, R> fn) {
        { httpResult -> Awaitable.of(httpResult.html as Document).then(fn) }
    }

    static <T, R> Task<HttpResult, R> jsonArray(Class<T> type, Function<List<T>, R> fn) {
        { httpResult -> Awaitable.of(httpResult.json)
                    .then { it as List<?> }
                    .then { list -> list.collect { it.asType(type) } }
                    .then(fn)
        }
    }

    static <T, R> Task<HttpResult, R> jsonObject(Class<T> type, Function<T, R> fn) {
        { httpResult -> Awaitable.of(httpResult.json)
                    .then { it.asType(type) }
                    .then(fn)
        }
    }
}
