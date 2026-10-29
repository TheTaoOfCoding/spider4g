package io.github.thetaoofcoding.example

import groovy.transform.ToString

@ToString(includeNames = true, includePackage = false)
class Book {
    Integer bookId
    String bookName
    String uri
    List<Chapter> chapters
    @ToString(includeNames = true, includePackage = false)
    static class Chapter {
        Integer orderId
        String uri
        String path
        String content
    }
}
