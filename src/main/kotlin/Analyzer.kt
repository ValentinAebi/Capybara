package com.github.valentinaebi.capybara

import org.objectweb.asm.ClassReader
import java.io.File
import java.nio.file.Files
import kotlin.system.exitProcess

private const val CLASS_FILE_EXT = ".class"


fun main(args: Array<String>) {

    val topLevelFiles = args.map { File(it) }

    val allFilesOk = checkFilesExist(topLevelFiles)
    if (!allFilesOk){
        exitProcess(-1)
    }

    for (topLevelFile in topLevelFiles) {
        for (file in topLevelFile.walk()) {
            if (file.isFile && file.name.endsWith(CLASS_FILE_EXT)){
                println("Analyzing ${file.name}")
                val bytes = Files.readAllBytes(file.toPath())
                val reader = ClassReader(bytes)
                val classAnalyzer = ClassAnalyzer()
                reader.accept(classAnalyzer, ClassReader.EXPAND_FRAMES)
            }
        }
    }

}

private fun checkFilesExist(files: List<File>): Boolean {
    var allFilesOk = true
    for (file in files) {
        if (!file.exists()) {
            allFilesOk = false
            System.err.println("file not found: ${file.name}")
        }
    }
    return allFilesOk
}

