package io.github.thetaoofcoding.component

import java.nio.file.Files
import java.nio.file.Paths

class FileTasks {
    static Task<String, String> x(String filePath, String content) {
        return { -> Files.writeString(Paths.get(filePath), content) }
    }
}
