package com.github.valentinaebi.capybara

import com.github.valentinaebi.capybara.loading.readClassFilesInDirTrees
import java.io.File


fun main(args: Array<String>) {
    val topLevelFiles = args.map { File(it) }
    val subtypeRel: SubtypingRelationBuilder = mutableMapOf()
    val classes = readClassFilesInDirTrees(topLevelFiles, subtypeRel)
    for (clazz in classes) {
        val className = clazz.className
        println("START CLASS $className")
        for ((methodName, method) in clazz.methods) {
            println("|\tMETHOD $className::$methodName")
            method.computeCfg()
            for (bb in method.cfg!!.basicBlocks) {
                println(bb.fullDescr().prependIndent("|\t¦\t"))
            }
            println("|\tEND METHOD $className::$methodName\n|")
        }
        println("END CLASS $className\n\n")
    }
}
