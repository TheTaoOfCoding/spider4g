package io.github.thetaoofcoding.component

import groovy.http.HttpResult
import org.jsoup.nodes.Document
import java.util.function.Function

class DocTasks {

    static Task<HttpResult, String> body(Function<String, String> function) {
        { httpResult ->
            def t = httpResult.body()
            async { function.apply(t) }
        }
    }

    static <R> Task<HttpResult, R> html(Function<Document, R> function) {
        { httpResult ->
            def doc = httpResult.html as Document
            async { function.apply(doc) }
        }
    }

    static <T> Task<HttpResult, String> jsonArray(Function<List<T>, String> function) {
        { httpResult ->
            def list = httpResult.json as List<T>
            async { function.apply(list) }
        }
    }

    static <T> Task<HttpResult, String> jsonObject(Function<T, String> function) {
        { httpResult ->
            def t = httpResult.json as T
            async { function.apply(t) }
        }
    }
}
