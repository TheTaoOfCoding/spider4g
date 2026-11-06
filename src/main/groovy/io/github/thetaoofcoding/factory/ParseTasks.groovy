package io.github.thetaoofcoding.factory

import groovy.concurrent.Awaitable
import groovy.json.JsonSlurper
import io.github.thetaoofcoding.component.Task
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ParseTasks {

    static Task<String, Document> doc() {
        { html -> Awaitable.of(html).then(Jsoup.&parse) }
    }

    static <R> Task<String, R> jsonArray(Class<R> type) {
        { str -> Awaitable.of(str)
                    .then(new JsonSlurper().&parseText)
                    .then { it as List<?> }
                    .then { list -> list.collect { it.asType(type) } }
        }
    }

    static <R> Task<String, R> jsonObj(Class<R> type) {
        { str -> Awaitable.of(str)
                    .then(new JsonSlurper().&parseText)
                    .then { it.asType(type) }
        }
    }
}
