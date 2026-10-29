package io.github.thetaoofcoding.example

import groovy.concurrent.Awaitable
import io.github.thetaoofcoding.component.DocTasks
import io.github.thetaoofcoding.component.Flow
import io.github.thetaoofcoding.component.HttpTasks
import io.github.thetaoofcoding.component.ParseTasks
import io.github.thetaoofcoding.component.Task
import org.jsoup.nodes.Document

class Example4QuanBen {

    // base uri
    static final String ORIGIN = 'https://www.quanben.io'
    // 章节列表选择器
    static final def CHAPTERS_SELECTOR = { Document doc ->
        doc.select('ul.list3 li a')
                .collect { "${ORIGIN}${it.attr('href')}" }
                .collect { uri -> new Book.Chapter(uri: uri) }
    }

    // 获取电子书详情
    static Flow<Closure, Book> bookSearchFlow(String bookName) {
        def httpTask = HttpTasks.httpGet()
        def srcTask = Task.fromSync { Document doc ->
            doc.select('div.list2 img')
                    .collect { new Tuple2<String, String>(it.attr('src'), it.attr('alt')) }
                    .find { it.v2 == bookName }
                    .v1
        }
        def subTask = Task.fromSync { String src -> src.substring(src.lastIndexOf('/') + 1, src.lastIndexOf('.')).replace('book_', '') }
        def bookIdTask = srcTask | subTask
        def hrefTask = Task.fromSync { Document doc ->
            doc.select('div.list2 h3 > a')
                    .collect { new Tuple2<String, String>(it.text(), it.attr('href')) }
                    .find { it -> it.v1 == bookName }
                    .v2
        }
        def composeTask = DocTasks.html { doc ->
            def (bookId, href) = await Awaitable.all(bookIdTask(doc), hrefTask(doc))
            new Book(bookId: bookId as Integer, bookName: bookName, uri: "${ORIGIN}${href}list.html", chapters: [])
        };
        { -> httpTask | composeTask }
    }

    // 不完整的章节列表[首尾] && callback
    static Flow<Closure, Tuple2<String, List<Book.Chapter>>> skeletonListFlow() {
        def httpTask = HttpTasks.httpGet()
        def scriptTask = Task.fromSync { Document doc ->
            doc.select('html head script').first().data()
        }
        def matchTask = Task.fromSync { String script ->
            def m = script =~ /var\s+callback\s*=\s*['"]([0-9a-f]{6})['"]/
            if (m.find()) return m.group(1)
        }
        def callbackTask = scriptTask | matchTask
        def docTask = DocTasks.html { doc ->
            def (callback, chapters) = await Awaitable.all(callbackTask(doc), Task.fromSync(CHAPTERS_SELECTOR)(doc))
            new Tuple2(callback, chapters)
        };
        { -> httpTask | docTask }
    }

    // 不完整的章节列表[中间]
    static Flow<Closure, List<Book.Chapter>> middleListFlow() {
        def httpTask = HttpTasks.httpGet()
        def docTask = DocTasks.body { body ->
            def startIndex = body.indexOf('(') + 1
            def endIndex = body.length() - 2
            body.trim().substring(startIndex, endIndex)
        }
        def jsonTask = ParseTasks.<Map<String, String>> json()
        def contentTask = Task.fromSync { it.content }
        def parseTask = ParseTasks.doc()
        def chaptersTask = Task.fromSync { Document doc -> CHAPTERS_SELECTOR(doc) };
        { -> httpTask | docTask | jsonTask | contentTask | parseTask | chaptersTask }
    }

    // 章节列表[全部]
    static Flow<Book, List<Book.Chapter>> chaptersFlow() {
        def flow1 = skeletonListFlow()
        def flow2 = middleListFlow();
        { ->
            { Book book ->
                def (callback, chapters1) = flow1 {
                    httpUri = book.uri
                }
                def chapters2 = flow2 {
                    httpUri = 'https://www.quanben.io/index.php'
                    httpHeaders = ['Referer': book.uri]
                    httpQueries = [c: 'book', a: 'list.jsonp', callback: callback, book_id: book.bookId, b: base64(callback)]
                }
                def chapters = chapters1 + chapters2
                chapters.each { chapter ->
                    def uri = chapter.uri
                    chapter.orderId = uri.substring(uri.lastIndexOf('/') + 1, chapter.uri.lastIndexOf('.')) as Integer
                    chapter.path = ''
                }
                book.chapters = chapters.sort { it.orderId }
                async { book }
            }
        }
    }

    // 章节内容
    static Flow<Closure, String> contentFlow() {
        def httpTask = HttpTasks.httpGet()
        def docTask = DocTasks.html { doc ->
            def title = doc.select('div.main h1').first().text()
            def content = doc.select('div#content p').collect { it.text() }.join('\n')
            "${title}\n${content}"
        };
        { -> httpTask | docTask }
    }

    static String base64(String str) {
        def staticChars = 'PXhw7UT1B0a9kQDKZsjIASmOezxYG4CHo5Jyfg2b8FLpEvRr3WtVnlqMidu6cN'
        def encodeChars = new StringBuilder()
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i)
            int num0 = staticChars.indexOf((int) c)
            def code
            if (num0 == -1) {
                code = String.valueOf(c)
            } else {
                code = String.valueOf(staticChars.charAt((num0 + 3) % 62))
            }
            int num1 = (int) (Math.random() * 62)
            int num2 = (int) (Math.random() * 62)
            encodeChars.append(staticChars.charAt(num1))
                    .append(code)
                    .append(staticChars.charAt(num2))
        }
        encodeChars.toString()
    }
}