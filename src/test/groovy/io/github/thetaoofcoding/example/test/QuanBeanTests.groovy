package io.github.thetaoofcoding.example.test

import io.github.thetaoofcoding.component.Flow
import io.github.thetaoofcoding.example.Book
import org.junit.jupiter.api.Test
import io.github.thetaoofcoding.example.Example4QuanBen

class QuanBeanTests {

    @Test
    void '获取电子书详情'() {
        def bookName = '武动乾坤'
        def flow = Example4QuanBen.bookSearchFlow(bookName)
        def book = flow {
            httpUri = 'https://www.quanben.io/index.php'
            httpQueries = [c: 'book', a: 'search', keywords: bookName]
        }
        println book
    }

    @Test
    void '获取章节头尾列表'() {
        def flow = Example4QuanBen.skeletonListFlow()
        def tuple2 = flow {
            httpUri = 'https://www.quanben.io/n/wudongqiankun/list.html'
        }
        println "章节头尾列表 = $tuple2"
    }

    @Test
    void '获取章节中间列表'() {
        def flow = Example4QuanBen.middleListFlow()
        def chapters = flow {
            httpUri = 'https://www.quanben.io/index.php'
            httpHeaders = ['Referer': 'https://www.quanben.io/n/wudongqiankun/list.html']
            httpQueries = [c: 'book', a: 'list.jsonp', callback: '0b0df2', book_id: 8, b: 'EkVOLE7kEOcZ9bRIFB']
        }
        println "章节中间列表 = $chapters"
    }

    @Test
    void '获取全部列表'() {
        def bookName = '武动乾坤'
        def flow1 = Example4QuanBen.bookSearchFlow(bookName)
        def flow2 = Example4QuanBen.chaptersFlow()
        def flow = flow1 | flow2
        def book = flow {
            httpUri = 'https://www.quanben.io/index.php'
            httpQueries = [c: 'book', a: 'search', keywords: bookName]
        }
        println book
    }

    @Test
    void '获取章节内容'() {
        def flow = Example4QuanBen.contentFlow()
        def content = flow {
            httpUri = 'https://www.quanben.io/n/wudongqiankun/1.html'
        }
        println content
    }
}
