package com.github.valentinaebi.capybara.loading

import com.github.valentinaebi.capybara.SubtypingRelationBuilder
import com.github.valentinaebi.capybara.programstruct.Class
import org.objectweb.asm.ClassReader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path


private const val CLASS_FILE_EXT = ".class"

fun readClassFile(path: Path, subtypeRel: SubtypingRelationBuilder): Class {
    val bytes = Files.readAllBytes(path)
    val reader = ClassReader(bytes)
    val classBuilder = ClassBuilder(subtypeRel)
    reader.accept(classBuilder, ClassReader.EXPAND_FRAMES)
    return classBuilder.toClass()
}

fun readClassFilesInDirTree(root: File, subtypeRel: SubtypingRelationBuilder): List<Class> {
    val classes = mutableListOf<Class>()
    for (file in root.walk()) {
        if (file.isFile && file.name.endsWith(CLASS_FILE_EXT)) {
            classes.add(readClassFile(file.toPath(), subtypeRel))
        }
    }
    return classes
}

fun readClassFilesInDirTrees(roots: List<File>, subtypeRel: SubtypingRelationBuilder): List<Class> =
    roots.flatMap { readClassFilesInDirTree(it, subtypeRel) }
