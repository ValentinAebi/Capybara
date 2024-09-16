package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.UNKNOWN_LINE_NUMBER
import com.github.valentinaebi.capybara.cfg.AssertionTerminator
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
import com.github.valentinaebi.capybara.checking.Reporter
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

private const val MAX_DEPTH = 100
private const val MAX_EXEC_CNT_FOR_SAME_BLOCK = 8

class Executor(
    private val interpreter: SymbolicInterpreter,
    private val solver: Solver,
    private val ctx: KContext,
    private val valuesCreator: ValuesCreator,
    private val reporter: Reporter
) {

    fun execute(method: Method): MethodSummary {
        reporter.currentMethod = method
        val cfg = method.cfg!!
        val frame = Frame<ProgramValue>(method.numLocals!!, method.maxStack!!)
        val params = populateFrameWithParams(method, frame)
        solver.push()
        if (method.hasReceiver) {
            solver.assert(ctx.mkNot(valuesCreator.areEqualFormula(params.first(), valuesCreator.nullValue)))
        }
        val results = dfsExecute(cfg.initialBasicBlock!!, frame, null, 0, mutableMapOf())
        solver.pop()
        return MethodSummary(method, params, results)
    }

    private fun dfsExecute(
        block: BasicBlock,
        frame: Frame<ProgramValue>,
        newAssumption: KExpr<KBoolSort>?,
        depth: Int,
        nExecPerBlock: MutableMap<BasicBlock, Int>
    ): Map<KExpr<KBoolSort>, MethodResult> {
        if (depth > MAX_DEPTH || !nExecPerBlock.allowsExecution(block)) {
            return emptyMap()
        }
        interpreter.lineResolver = { block.insnList[it] ?: UNKNOWN_LINE_NUMBER }
        if (newAssumption != null) {
            solver.push()
            solver.assert(newAssumption)
        }
        val results: MutableMap<KExpr<KBoolSort>, MethodResult> = mutableMapOf()
        if (solver.isConsistent()) {
            nExecPerBlock.incrementExecCnt(block)
            block.simulateInstructions(frame, interpreter)
            when (val terminatorRes = interpretTerminator(block.terminator, frame, ctx, valuesCreator)) {
                is PossiblePaths -> {
                    for ((block, newConstraint) in terminatorRes.regularPaths) {
                        val newFrame = Frame<ProgramValue>(frame)
                        val subResults = dfsExecute(block, newFrame, newConstraint, depth + 1, nExecPerBlock)
                        results.putAll(subResults)
                    }
                    if (terminatorRes.exceptionAndCondition != null) {
                        val (exc, failureCond) = terminatorRes.exceptionAndCondition
                        val fullCond = ctx.mkAnd(failureCond, *solver.currentlyActiveFormulas.toTypedArray())
                        results[fullCond] = ThrowResult(exc)
                    }
                }

                is Return -> {
                    val cond = ctx.mkAnd(solver.currentlyActiveFormulas)
                    results[cond] = ReturnResult(terminatorRes.returnedValue)
                }
            }
            nExecPerBlock.decrementExecCnt(block)
        }
        if (newAssumption != null) {
            solver.pop()
        }
        return results
    }

    private fun interpretTerminator(
        terminator: BasicBlockTerminator,
        frame: Frame<ProgramValue>,
        ctx: KContext,
        valuesCreator: ValuesCreator
    ): TerminatorInterpretationResult = with(ctx) {
        with(valuesCreator) {
            return when {

                terminator is IteTerminator && terminator.cond is UnaryOperandStackPredicate -> {
                    val value = frame.pop()
                    val constraint = constraintFor(terminator.cond, value, valuesCreator)
                    val paths = listOf(
                        terminator.successorIfTrue to constraint,
                        terminator.successorIfFalse to ctx.mkNot(constraint)
                    )
                    PossiblePaths(paths, null)
                }

                terminator is IteTerminator && terminator.cond is BinaryOperandStackPredicate -> {
                    val r = frame.pop()
                    val l = frame.pop()
                    val constraint = constraintFor(terminator.cond, l, r, valuesCreator)
                    val paths = listOf(
                        terminator.successorIfTrue to constraint,
                        terminator.successorIfFalse to mkNot(constraint)
                    )
                    PossiblePaths(paths, null)
                }

                terminator is ReturnTerminator -> {
                    val result = if (terminator.mustPopValue) frame.pop() else null
                    assert(frame.stackSize == 0)
                    Return(result)
                }

                terminator is SingleSuccessorTerminator -> PossiblePaths(
                    listOf(terminator.successor to ctx.mkTrue()),
                    null
                )

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
                    PossiblePaths(nextPaths, null)
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
                    PossiblePaths(nextPaths, null)
                }

                terminator == ThrowTerminator -> {
                    val exception = frame.pop().ref()
                    PossiblePaths(emptyList(), exception to ctx.mkTrue())
                }

                terminator is AssertionTerminator -> {
                    val correctnessFormula = terminator.mkCorrectnessFormula(frame, valuesCreator, ctx)
                    val failureFormula = ctx.mkNot(correctnessFormula)
                    val exception = valuesCreator.mkSymbolicRef("intrinsic_exception")
                    val canProveFailure = solver.canProve(failureFormula)
                    if (canProveFailure) {
                        reporter.report(terminator.check)
                    }
                    val canProveSuccess = !canProveFailure && solver.canProve(correctnessFormula)
                    PossiblePaths(
                        listOf(terminator.successor to correctnessFormula),
                        if (canProveSuccess || canProveFailure) null else exception to failureFormula
                    )
                }

                else -> throw AssertionError("unexpected terminator: ${terminator.fullDescr()}")
            }

        }
    }

    private sealed interface TerminatorInterpretationResult

    private data class PossiblePaths(
        val regularPaths: List<Pair<BasicBlock, KExpr<KBoolSort>>>,
        val exceptionAndCondition: Pair<ReferenceValue, KExpr<KBoolSort>>?
    ) : TerminatorInterpretationResult

    private data class Return(val returnedValue: ProgramValue?) : TerminatorInterpretationResult

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
    ): List<ProgramValue> {
        val params = mutableListOf<ProgramValue>()
        var localIdx = 0
        if (method.hasReceiver) {
            val receiverParam = valuesCreator.mkSymbolicRef("recv")
            params.add(receiverParam)
            frame.setLocal(localIdx, receiverParam)
            localIdx += 1
        }
        val paramTypes = Type.getType(method.methodNode.desc).argumentTypes
        for ((idx, paramType) in paramTypes.withIndex()) {
            val param = valuesCreator.mkSymbolicValue(paramType.sort, "arg$idx")
            params.add(param)
            frame.setLocal(localIdx, param)
            localIdx += paramType.size
        }
        return params
    }

    private fun MutableMap<BasicBlock, Int>.incrementExecCnt(block: BasicBlock) {
        val cntBefore = getOrPut(block) { 0 }
        this[block] = cntBefore + 1
    }

    private fun MutableMap<BasicBlock, Int>.decrementExecCnt(block: BasicBlock) {
        this[block] = this[block]!! - 1
    }

    private fun MutableMap<BasicBlock, Int>.allowsExecution(block: BasicBlock): Boolean {
        val cnt = this[block]
        return cnt == null || cnt < MAX_EXEC_CNT_FOR_SAME_BLOCK
    }

}
