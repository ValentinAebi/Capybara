package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.UNKNOWN_LINE_NUMBER
import com.github.valentinaebi.capybara.cfg.BasicBlock
import com.github.valentinaebi.capybara.cfg.BasicBlockTerminator
import com.github.valentinaebi.capybara.cfg.BinaryOperandStackPredicate
import com.github.valentinaebi.capybara.cfg.IteTerminator
import com.github.valentinaebi.capybara.cfg.LookupSwitchTerminator
import com.github.valentinaebi.capybara.cfg.ReturnTerminator
import com.github.valentinaebi.capybara.cfg.SingleSuccessorTerminator
import com.github.valentinaebi.capybara.cfg.TableSwitchTerminator
import com.github.valentinaebi.capybara.cfg.ThrowTerminator
import com.github.valentinaebi.capybara.cfg.UnaryOperandStackPredicate
import com.github.valentinaebi.capybara.checks.Reporter
import com.github.valentinaebi.capybara.programstruct.Method
import com.github.valentinaebi.capybara.solving.Solver
import com.github.valentinaebi.capybara.values.NumericValue
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort
import org.objectweb.asm.Type
import org.objectweb.asm.tree.analysis.Frame

private const val MAX_EXEC_CNT_FOR_SAME_BLOCK = 8

class Executor(
    private val interpreter: SymbolicInterpreter,
    private val solver: Solver,
    private val ctx: KContext,
    private val valuesCreator: ValuesCreator,
    private val reporter: Reporter
) {

    fun execute(method: Method) {
        if (method.isAbstract) {
            return
        }
        reporter.currentMethod = method
        val cfg = method.cfg!!
        val frame = Frame<ProgramValue>(method.numLocals!!, method.maxStack!!)
        populateFrameWithParams(method, frame)
        dfsExecute(cfg.initialBasicBlock!!, frame, null, 0, mutableMapOf())
    }

    private fun dfsExecute(
        block: BasicBlock,
        frame: Frame<ProgramValue>,
        newAssumption: KExpr<KBoolSort>?,
        depth: Int,
        nExecPerBlock: MutableMap<BasicBlock, Int>
    ) {
        if (!nExecPerBlock.canExecute(block)) {
            return
        }
        interpreter.lineResolver = { block.insnList[it] ?: UNKNOWN_LINE_NUMBER }
        if (newAssumption != null) {
            solver.push()
            solver.assert(newAssumption)
        }
        if (solver.isConsistent()) {
            nExecPerBlock.incrementExecCnt(block)
            try {
                block.simulateInstructions(frame, interpreter)
                val nextPaths = interpretTerminator(block.terminator, frame, ctx, valuesCreator)
                for ((block, newConstraint) in nextPaths) {
                    val newFrame = Frame<ProgramValue>(frame)
                    dfsExecute(block, newFrame, newConstraint, depth + 1, nExecPerBlock)
                }
            } catch (_: ThrowEvent) {
                // TODO also simulate exceptional paths
            } finally {
                nExecPerBlock.decrementExecCnt(block)
            }
        }
        if (newAssumption != null) {
            solver.pop()
        }
    }

    private fun interpretTerminator(
        terminator: BasicBlockTerminator,
        frame: Frame<ProgramValue>,
        ctx: KContext,
        valuesCreator: ValuesCreator
    ): List<Pair<BasicBlock, KExpr<KBoolSort>?>> = with(ctx) {
        with(valuesCreator) {
            return when {

                terminator is IteTerminator && terminator.cond is UnaryOperandStackPredicate -> {
                    val value = frame.pop()
                    val constraint = constraintFor(terminator.cond, value, valuesCreator)
                    listOf(
                        terminator.successorIfTrue to constraint,
                        terminator.successorIfFalse to ctx.mkNot(constraint)
                    )
                }

                terminator is IteTerminator && terminator.cond is BinaryOperandStackPredicate -> {
                    val r = frame.pop()
                    val l = frame.pop()
                    val constraint = constraintFor(terminator.cond, l, r, valuesCreator)
                    listOf(
                        terminator.successorIfTrue to constraint,
                        terminator.successorIfFalse to mkNot(constraint)
                    )
                }

                terminator is ReturnTerminator -> {
                    if (terminator.mustPopValue) {
                        frame.pop()
                    }
                    assert(frame.stackSize == 0)
                    emptyList()
                }

                terminator is SingleSuccessorTerminator -> listOf(terminator.successor to null)

                terminator is TableSwitchTerminator -> {
                    val selector = frame.pop().int32().ksmtValue
                    val dflt = terminator.default
                    val nextPaths = mutableListOf<Pair<BasicBlock, KExpr<KBoolSort>>>()
                    val conditionsLeadingToDefault = mutableListOf<KExpr<KBoolSort>>()
                    var key = terminator.minKey
                    conditionsLeadingToDefault.add(mkArithLt(selector, key.expr))
                    for (case in terminator.cases) {
                        val formula = selector eq key.expr
                        if (case == dflt) {
                            conditionsLeadingToDefault.add(formula)
                        } else {
                            nextPaths.add(case to formula)
                        }
                        key += 1
                    }
                    conditionsLeadingToDefault.add(mkArithLt(key.expr, selector))
                    nextPaths.add(dflt to mkOr(conditionsLeadingToDefault))
                    nextPaths
                }

                terminator is LookupSwitchTerminator -> {
                    val selector = frame.pop().int32().ksmtValue
                    val nextPaths = mutableListOf<Pair<BasicBlock, KExpr<KBoolSort>>>()
                    for ((key, block) in terminator.cases) {
                        val key = (key as Int).expr
                        val formula = selector eq key
                        nextPaths.add(block to formula)
                    }
                    val conditionsLeadingToNonDefault = nextPaths.map { it.second }
                    val defaultCond = mkNot(mkOr(conditionsLeadingToNonDefault))
                    nextPaths.add(terminator.default to defaultCond)
                    nextPaths
                }

                terminator == ThrowTerminator -> {
                    frame.pop()
                    // TODO consider catches (and pop the right number of times)
                    emptyList()
                }

                else -> throw AssertionError("unexpected terminator: ${terminator.fullDescr()}")
            }

        }
    }

    private fun constraintFor(
        pred: UnaryOperandStackPredicate,
        value: ProgramValue,
        valuesCreator: ValuesCreator
    ): KExpr<KBoolSort> {
        return when (pred) {
            UnaryOperandStackPredicate.IsZero -> valuesCreator.isZeroFormula(value as NumericValue<*>)
            UnaryOperandStackPredicate.LessThanZero -> valuesCreator.isLessThanZeroFormula(value as NumericValue<*>)
            UnaryOperandStackPredicate.GreaterThanZero -> valuesCreator.isGreaterThanZeroFormula(value as NumericValue<*>)
            UnaryOperandStackPredicate.IsNull -> with(valuesCreator) { value.ref() eq nullValue }
        }
    }

    private fun constraintFor(
        pred: BinaryOperandStackPredicate,
        l: ProgramValue, r: ProgramValue,
        valuesCreator: ValuesCreator
    ): KExpr<KBoolSort> {
        return when (pred) {
            BinaryOperandStackPredicate.Equal -> valuesCreator.areEqualFormula(l, r)

            BinaryOperandStackPredicate.LessThan ->
                valuesCreator.lessThanFormula(l as NumericValue<*>, r as NumericValue<*>)

            BinaryOperandStackPredicate.GreaterThan ->
                valuesCreator.lessThanFormula(r as NumericValue<*>, l as NumericValue<*>)
        }
    }

    private fun populateFrameWithParams(
        method: Method,
        frame: Frame<ProgramValue>
    ) {
        val argTypes = Type.getType(method.methodNode.desc).argumentTypes
        var localIdx = 0
        if (method.hasReceiver) {
            frame.setLocal(localIdx, valuesCreator.mkSymbolicRef("recv"))
            localIdx += 1
        }
        for ((idx, argType) in argTypes.withIndex()) {
            frame.setLocal(localIdx, valuesCreator.mkSymbolicValue(argType.sort, "arg$idx"))
            localIdx += argType.size
        }
    }

    private fun MutableMap<BasicBlock, Int>.incrementExecCnt(block: BasicBlock) {
        val cntBefore = getOrPut(block) { 0 }
        this[block] = cntBefore + 1
    }

    private fun MutableMap<BasicBlock, Int>.decrementExecCnt(block: BasicBlock) {
        this[block] = this[block]!! - 1
    }

    private fun MutableMap<BasicBlock, Int>.canExecute(block: BasicBlock): Boolean {
        val cnt = this[block]
        return cnt == null || cnt < MAX_EXEC_CNT_FOR_SAME_BLOCK
    }

}
