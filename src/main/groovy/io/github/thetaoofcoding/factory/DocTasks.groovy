package io.github.thetaoofcoding.factory

import groovy.concurrent.Awaitable
import groovy.http.HttpResult
import io.github.thetaoofcoding.component.Task
import org.jsoup.nodes.Document
import java.util.function.Function

class DocTasks {

    static <R> Task<HttpResult, R> body(Function<String, R> function) {
        { httpResult -> Awaitable.of(httpResult.body()).then(function) }
    }

    static <R> Task<HttpResult, R> html(Function<Document, R> function) {
        { httpResult -> Awaitable.of(httpResult.html as Document).then(function) }
    }

    static <T, R> Task<HttpResult, R> jsonArray(Function<List<T>, R> function) {
        { httpResult -> Awaitable.of(httpResult.json as List<T>).then(function) }
    }

    static <T, R> Task<HttpResult, R> jsonObject(Function<T, R> function) {
        { httpResult -> Awaitable.of(httpResult.json as T).then(function) }
    }
}
