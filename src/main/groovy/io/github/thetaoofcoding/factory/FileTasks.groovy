package io.github.thetaoofcoding.factory

import groovy.concurrent.Awaitable
import io.github.thetaoofcoding.component.Task
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

    class FileTasks {

        static Task<Tuple2<Path, String>, Path> saveFile() {
            { tuple2 ->
                // 文件路径
                def filePath = tuple2.v1
                // 文件内容
                def content = tuple2.v2
                // 目录路径
                def directoryPath = filePath.parent

                if (Files.notExists(directoryPath)) {
                    synchronized (directoryPath) {
                        if (Files.notExists(directoryPath)) Files.createDirectories(directoryPath)
                    }
                }
                Files.writeString(filePath, content)
                Awaitable.of(filePath)
            }
        }

        static Task<Tuple2<Path, Path>, Path> appendFileTo() {
            { tuple2 ->
                // 源文件路径
                def sourcePath = tuple2.v1
                // 目标文件路径
                def targetPath = tuple2.v2
                // 读取源文件内容
                def content = Files.readString(sourcePath)
                // 追加内容到目标文件
                Files.writeString(targetPath, "$content\n\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
                // 待删除目录
                Awaitable.of(sourcePath)
            }
        }

        static Task<Path, Void> cleanFile() {
            { path ->
                Files.deleteIfExists(path)
                Awaitable.of(null)
            }
        }
    }
