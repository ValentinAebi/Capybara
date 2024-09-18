package com.github.valentinaebi.capybara.loading

import com.github.valentinaebi.capybara.GraphBuilder
import com.github.valentinaebi.capybara.InternalName
import com.github.valentinaebi.capybara.programstruct.Class
import com.github.valentinaebi.capybara.programstruct.MethodIdentifier
import org.objectweb.asm.ClassReader
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

private const val CLASS_FILE_EXT = ".class"

fun readClassFile(
    path: Path,
    subtypingRelation: GraphBuilder<InternalName>,
    callGraph: GraphBuilder<MethodIdentifier>
): Class {
    if (!path.toFile().exists()) {
        throw IOException("class file not found: $path")
    }
    val bytes = Files.readAllBytes(path)
    val reader = ClassReader(bytes)
    val classBuilder = ClassBuilder(subtypingRelation, callGraph)
    reader.accept(classBuilder, ClassReader.EXPAND_FRAMES)
    return classBuilder.toClass()
}

fun readClassFilesInDirTree(
    root: File,
    subtypingMap: GraphBuilder<InternalName>,
    callGraph: GraphBuilder<MethodIdentifier>
): List<Class> {
    if (!root.exists()) {
        throw IOException("root not found: ${root.path}")
    }
    val classes = mutableListOf<Class>()
    for (file in root.walk()) {
        if (file.isFile && file.name.endsWith(CLASS_FILE_EXT)) {
            classes.add(readClassFile(file.toPath(), subtypingMap, callGraph))
        }
    }
    return classes
}

fun readClassFilesInDirTrees(
    roots: List<File>,
    subtypingRelation: GraphBuilder<InternalName>,
    callGraph: GraphBuilder<MethodIdentifier>
): List<Class> =
    roots.flatMap { readClassFilesInDirTree(it, subtypingRelation, callGraph) }
