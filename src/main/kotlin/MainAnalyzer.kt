package com.github.valentinaebi.capybara

import com.github.valentinaebi.capybara.checking.Reporter
import com.github.valentinaebi.capybara.loading.readClassFilesInDirTrees
import com.github.valentinaebi.capybara.programstruct.MethodIdentifier
import com.github.valentinaebi.capybara.solving.Solver
import com.github.valentinaebi.capybara.symbolicexecution.Executor
import com.github.valentinaebi.capybara.symbolicexecution.SymbolicInterpreter
import com.github.valentinaebi.capybara.values.OperatorsContext
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import java.io.File


fun main(args: Array<String>) {

    val timer = Timer()
    val topLevelFiles = args.map { File(it) }
    val subtypeRelationBuilder = GraphBuilder<InternalName>()
    val callGraphBuilder = GraphBuilder<MethodIdentifier>()

    // Load class files
    timer.reset()
    val classes = readClassFilesInDirTrees(topLevelFiles, subtypeRelationBuilder, callGraphBuilder)
    val loadingTime = timer.elapsedTime()

    // Build CFGs
    timer.reset()
    classes.forEach { it.methods.values.forEach { it.computeCfgWithAssertions() } }
    val cfgComputationTime = timer.elapsedTime()

    // Display CFGs
    for (clazz in classes) {
        val className = clazz.className
        println("START CLASS $className")
        for (method in clazz.methods.values) {
            val methodId = method.methodId
            println("|\tSTART METHOD $methodId")
            for (bb in method.cfg!!.basicBlocks) {
                println(bb.fullDescr().prependIndent("|\t¦\t"))
            }
            println("|\tEND METHOD $methodId\n|")
        }
        println("END CLASS $className\n\n")
    }
    println()

    // Setup symbolic execution system
    timer.reset()
    val reporter = Reporter()
    val ctx = KContext()
    val valuesCreator = ValuesCreator(ctx)
    val operatorsContext = OperatorsContext(ctx)
    val solver = Solver(ctx, valuesCreator)
    val interpreter = SymbolicInterpreter(reporter, valuesCreator, operatorsContext, solver)
    val executor = Executor(interpreter, solver, ctx, valuesCreator, reporter)
    val symbolicExecutionSetupTime = timer.elapsedTime()

    // Run symbolic execution of methods
    timer.reset()
    for (clazz in classes) {
        reporter.currentClass = clazz
        for ((_, method) in clazz.methods) {
            if (!method.isAbstract) {
                val summary = executor.execute(method)
                println(summary)
                println()
            }
        }
    }
    val symbolicExecutionTime = timer.elapsedTime()

    // Display results and statistics
    println("\n ---------- Analysis results ---------- ")
    reporter.dumpIssues(::println)
    println("\n ------------- Statistics ------------- ")
    println("  - Loading:            $loadingTime")
    println("  - CFGs building:      $cfgComputationTime")
    println("  - Symbolic execution setup: $symbolicExecutionSetupTime")
    println("  - Symbolic execution: $symbolicExecutionTime")
}
