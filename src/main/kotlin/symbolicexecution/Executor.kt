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
import com.github.valentinaebi.capybara.values.ReferenceValue
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort
import org.objectweb.asm.Type
import org.objectweb.asm.tree.analysis.Frame

private const val MAX_DEPTH = 15

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
        dfsExecute(cfg.initialBasicBlock!!, frame, null, 0)
    }

    private fun dfsExecute(
        block: BasicBlock,
        frame: Frame<ProgramValue>,
        newAssumption: KExpr<KBoolSort>?,
        depth: Int
    ) {
        if (depth > MAX_DEPTH) {
            return
        }
        interpreter.lineResolver = { block.insnList[it] ?: UNKNOWN_LINE_NUMBER }
        if (newAssumption != null) {
            solver.push()
            solver.assert(newAssumption)
        }
        if (solver.isConsistent()) {
            var executionCompleted: Boolean
            try {
                block.simulateInstructions(frame, interpreter)
                executionCompleted = true
            } catch (_ : ThrowEvent){
                // TODO also simulate exceptional paths
                executionCompleted = false
            }
            if (executionCompleted) {
                val nextPaths = interpretTerminator(block.terminator, frame, ctx, valuesCreator)
                for ((block, newConstraint) in nextPaths) {
                    val newFrame = Frame<ProgramValue>(frame)
                    dfsExecute(block, newFrame, newConstraint, depth + 1)
                }
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
    ): List<Pair<BasicBlock, KExpr<KBoolSort>?>> {
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
                    terminator.successorIfFalse to ctx.mkNot(constraint)
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
            terminator is TableSwitchTerminator -> TODO()
            terminator is LookupSwitchTerminator -> TODO()
            terminator == ThrowTerminator -> {
                frame.pop()
                // TODO consider catches (and pop the right number of times)
                emptyList()
            }

            else -> throw AssertionError("unexpected terminator: ${terminator.fullDescr()}")
        }

    }

    private fun constraintFor(
        pred: UnaryOperandStackPredicate,
        value: ProgramValue,
        valuesCreator: ValuesCreator
    ): KExpr<KBoolSort> {
        return when (pred) {
            UnaryOperandStackPredicate.IsZero -> valuesCreator.isZeroConstraint(value as NumericValue<*>)
            UnaryOperandStackPredicate.LessThanZero -> valuesCreator.isLessThanZeroConstraint(value as NumericValue<*>)
            UnaryOperandStackPredicate.GreaterThanZero -> valuesCreator.isGreaterThanZeroConstraint(value as NumericValue<*>)
            UnaryOperandStackPredicate.IsNull -> with(valuesCreator) { (value as ReferenceValue) eq nullValue }
        }
    }

    private fun constraintFor(
        pred: BinaryOperandStackPredicate,
        l: ProgramValue, r: ProgramValue,
        valuesCreator: ValuesCreator
    ): KExpr<KBoolSort> {
        return when (pred) {
            BinaryOperandStackPredicate.Equal -> valuesCreator.areEqualConstraint(l, r)

            BinaryOperandStackPredicate.LessThan ->
                valuesCreator.lessThanConstraint(l as NumericValue<*>, r as NumericValue<*>)

            BinaryOperandStackPredicate.GreaterThan ->
                valuesCreator.lessThanConstraint(r as NumericValue<*>, l as NumericValue<*>)
        }
    }

    private fun populateFrameWithParams(
        method: Method,
        frame: Frame<ProgramValue>
    ) {
        val argTypes = Type.getType(method.methodNode.desc).argumentTypes
        var localIdx = 0
        if (method.hasReceiver) {
            frame.setLocal(localIdx, valuesCreator.mkSymbolicRef())
            localIdx += 1
        }
        for (argType in argTypes) {
            frame.setLocal(localIdx, valuesCreator.mkSymbolicValue(argType.sort))
            localIdx += argType.size
        }
    }

}
