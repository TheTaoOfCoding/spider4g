package io.github.thetaoofcoding.component

import groovy.json.JsonSlurper
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
