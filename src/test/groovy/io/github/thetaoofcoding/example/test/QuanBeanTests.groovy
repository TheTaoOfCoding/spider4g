package io.github.thetaoofcoding.example.test

import org.junit.jupiter.api.Test
import io.github.thetaoofcoding.example.Example4QuanBen

class QuanBeanTests {

//    @Test
//    void '获取电子书详情'() {
//        def bookName = '武动乾坤'
//        def flow = Example4QuanBen.bookSearchFlow(bookName)
//        def book = flow {
//            httpUri = 'https://www.quanben.io/index.php'
//            httpQueries = [c: 'book', a: 'search', keywords: bookName]
//        }
//        println book
//    }

    @Test
    void '获取全部列表'() {
        def bookName = '大主宰'
        def flow1 = Example4QuanBen.bookSearchFlow(bookName)
        def flow2 = Example4QuanBen.chapterListFlow()
        def flow3 = Example4QuanBen.contentListFlow()
        def flow4 = Example4QuanBen.mergeAndCleanFlow()
        def flow = flow1 | flow2 | flow3 | flow4
        def book = flow {
            httpUri = 'https://www.quanben.io/index.php'
            httpQueries = [c: 'book', a: 'search', keywords: bookName]
        }
        println book
    }

    @Test
    void '获取章节内容'() {
        Example4QuanBen.contentFlow()() {
            httpUri = 'https://www.quanben.io/n/wudongqiankun/1.html'
        }
    }
}
