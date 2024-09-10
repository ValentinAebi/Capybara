package com.github.valentinaebi.capybara

import com.github.valentinaebi.capybara.checks.Reporter
import com.github.valentinaebi.capybara.loading.readClassFilesInDirTrees
import com.github.valentinaebi.capybara.solving.Solver
import com.github.valentinaebi.capybara.symbolicexecution.Checker
import com.github.valentinaebi.capybara.symbolicexecution.Executor
import com.github.valentinaebi.capybara.symbolicexecution.OperatorsContext
import com.github.valentinaebi.capybara.symbolicexecution.SymbolicInterpreter
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import java.io.File
import kotlin.time.measureTime
import kotlin.time.measureTimedValue


fun main(args: Array<String>) {
    val topLevelFiles = args.map { File(it) }
    val subtypeRel: SubtypingRelationBuilder = mutableMapOf()
    val (classes, loadingTime) = measureTimedValue {
        readClassFilesInDirTrees(topLevelFiles, subtypeRel)
    }
    val cfgComputationTime = measureTime {
        classes.forEach { it.methods.values.forEach { it.computeCfg() } }
    }
    for (clazz in classes) {
        val className = clazz.className
        println("START CLASS $className")
        for ((methodName, method) in clazz.methods) {
            println("|\tMETHOD $className::$methodName")
            for (bb in method.cfg!!.basicBlocks) {
                println(bb.fullDescr().prependIndent("|\t¦\t"))
            }
            println("|\tEND METHOD $className::$methodName\n|")
        }
        println("END CLASS $className\n\n")
    }
    println()
    val reporter = Reporter()
    val symbolicExecutionTime = measureTime {
        val ctx = KContext()
        val valuesCreator = ValuesCreator(ctx)
        val operatorsContext = OperatorsContext(ctx)
        val solver = Solver(ctx, valuesCreator)
        val checker = Checker(reporter, solver)
        val interpreter = SymbolicInterpreter(reporter, valuesCreator, operatorsContext, checker)
        val executor = Executor(interpreter, solver, ctx, valuesCreator, reporter)
        for (clazz in classes) {
            reporter.currentClass = clazz
            for ((_, method) in clazz.methods) {
                executor.execute(method)
            }
        }
    }
    println("\n ---------- Analysis results ---------- ")
    reporter.printReport(System.out)
    println("\nStats:")
    println("  - Loading:            $loadingTime")
    println("  - CFGs building:      $loadingTime")
    println("  - Symbolic execution: $symbolicExecutionTime")
}
