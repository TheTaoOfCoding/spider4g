package io.github.thetaoofcoding.factory

import groovy.json.JsonSlurper
import io.github.thetaoofcoding.component.Task
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ParseTasks {

    static Task<String, Document> doc() {
        { str -> async { Jsoup.parse(str) } }
    }

    static <R> Task<String, R> json() {
        { str -> async { new JsonSlurper().parseText(str) as R } }
    }
}
