package io.github.thetaoofcoding.example

import groovy.concurrent.Awaitable
import groovy.http.HttpResult
import io.github.thetaoofcoding.factory.DocTasks
import io.github.thetaoofcoding.component.Flow
import io.github.thetaoofcoding.factory.FileTasks
import io.github.thetaoofcoding.factory.HttpTasks
import io.github.thetaoofcoding.component.Task
import org.jsoup.nodes.Document
import java.nio.file.Path
import java.nio.file.Paths
import static io.github.thetaoofcoding.example.Example4QuanBen.QuanBenFlows.*

class Example4QuanBen {

    static void main(String[] args) {
        (bookSearchFlow() >> chapterListFlow() >> contentListFlow() >> mergeAndCleanFlow())("极品修真邪少")
    }

    static class QuanBenFlows {
        // base uri
        static final String ORIGIN = 'https://www.quanben.io'
        // 是否测试模式
        static final boolean IS_TEST = true

        // 搜索电子书流程
        static Flow<String, Book> bookSearchFlow() {
            { ->
                { bookName ->
                    // 解析页面
                    def t1 = DocTasks.html { doc ->
                        // 电子书id
                        def bookId = doc.select('div.list2 img')
                                .collect { new Tuple2<String, String>(it.attr('src'), it.attr('alt')) }
                                .find { it.v2 == bookName }
                                .v1
                                .with { it.substring(it.lastIndexOf('/') + 1, it.lastIndexOf('.')).replace('book_', '') }
                        // 电子书短路径
                        def path = doc.select('div.list2 h3 > a')
                                .collect { new Tuple2<String, String>(it.text(), it.attr('href')) }
                                .find { it -> it.v1 == bookName }
                                .v2
                        new Tuple2<>(bookId, path)
                    }
                    // 转换
                    def t2 = Task.<Tuple2<String, String>, Book> fromSync { new Book(bookId: it.v1 as Integer, bookName: bookName, uri: "${ORIGIN}/amp${it.v2}list.html") }
                    (HttpTasks.httpGet() | t1 | t2) {
                        httpUri = 'https://www.quanben.io/index.php'
                        httpQueries = [c: 'book', a: 'search', keywords: bookName]
                    }
                }
            }
        }

        // 获取完整章节列表流程
        static Flow<Book, Book> chapterListFlow() {
            def t1 = DocTasks.html { Document doc ->
                doc.select('ul.list3 li a')
                        .collect { "${ORIGIN}${it.attr('href')}" }
                        .collect { uri -> new Book.Chapter(orderId: uri.substring(uri.lastIndexOf('/') + 1, uri.lastIndexOf('.')) as Integer, uri: uri) }
            };
            { ->
                { book ->
                    def t2 = Task.<List<Book.Chapter>, Book> fromSync { book.chapters = it; book }
                    (HttpTasks.httpGet() | t1 | t2) { httpUri = book.uri }
                }
            }
        }

        // 获取章节内容流程【单章节】
        static Flow<Closure, Path> contentFlow() {
            def t1 = DocTasks.html { doc ->
                def title = doc.select('div.main h1').first().text()
                def href = doc.select('html head link').first().attr('href')
                def name = doc.select('div.name').first().text()
                def orderId = href.substring(href.lastIndexOf('/') + 1, href.lastIndexOf('.')) as Integer
                def content = doc.select('div.articlebody p').collect { it.text() }.join('\n').with { "${title}\n${it}" }
                def path = Paths.get("D:/$name/${orderId}.txt")
                new Tuple2<>(path, content)
            }
            // 保存文件任务
            def t2 = FileTasks.saveFile();
            // 限流
            { -> Task.<Closure, HttpResult> withRateLimit(HttpTasks.httpGet(), 2L) | t1 | t2 }
        }

        // 获取章节内容流程【完整】
        static Flow<Book, Tuple2<Path, List<Path>>> contentListFlow() {
            final def flow = Flow.<Closure, Path> withParallel({ IS_TEST ? it.take(20) : it }, contentFlow(), { it });
            { ->
                { book ->
                    def files = flow(book.chapters.collect { chapter -> { httpUri = chapter.uri } as Closure })
                    def directoryPath = Paths.get("D:/${book.bookName}")
                    def targetPath = directoryPath.resolve("${book.bookName}.txt")
                    def tuple2 = new Tuple2<>(targetPath, files)
                    Awaitable.of(tuple2)
                }
            }
        }

        // 合并并清理文件
        static Flow<Tuple2<Path, List<Path>>, Void> mergeAndCleanFlow() {
            def flow = { -> FileTasks.appendFileTo() | FileTasks.cleanFile() } as Flow<Tuple2<Path, Path>, Void>
            def parallelFlow = Flow.withParallel({ it }, flow, { it });
            { ->
                { tuple2 ->
                    def (targetPath, files) = tuple2
                    def tuples = files.collect { new Tuple2<>(it, targetPath) }
                    parallelFlow(tuples)
                    Awaitable.of(null)
                }
            }
        }
    }
}