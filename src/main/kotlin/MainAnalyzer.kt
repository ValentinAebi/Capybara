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


fun main(args: Array<String>) {
    val timer = Timer()
    val topLevelFiles = args.map { File(it) }
    val subtypeRel: SubtypingRelationBuilder = mutableMapOf()
    timer.start()
    val classes = readClassFilesInDirTrees(topLevelFiles, subtypeRel)
    val loadingTime = timer.stop()
    timer.start()
    classes.forEach { it.methods.values.forEach { it.computeCfg() } }
    val cfgComputationTime = timer.stop()
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
    timer.start()
    val reporter = Reporter()
    val ctx = KContext()
    val valuesCreator = ValuesCreator(ctx)
    val operatorsContext = OperatorsContext(ctx)
    val solver = Solver(ctx, valuesCreator)
    val checker = Checker(reporter, solver)
    val interpreter = SymbolicInterpreter(reporter, valuesCreator, operatorsContext, checker)
    val executor = Executor(interpreter, solver, ctx, valuesCreator, reporter)
    val symbolicExecutionSetupTime = timer.stop()
    timer.start()
    for (clazz in classes) {
        reporter.currentClass = clazz
        for ((_, method) in clazz.methods) {
            executor.execute(method)
        }
    }
    val symbolicExecutionTime = timer.stop()
    println("\n ---------- Analysis results ---------- ")
    reporter.printReport(System.out)
    println("\nStatistics:")
    println("  - Loading:            $loadingTime")
    println("  - CFGs building:      $cfgComputationTime")
    println("  - Symbolic execution setup: $symbolicExecutionSetupTime")
    println("  - Symbolic execution: $symbolicExecutionTime")
}
