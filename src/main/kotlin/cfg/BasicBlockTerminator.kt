package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.checking.Check
import com.github.valentinaebi.capybara.programstruct.MethodIdentifier
import com.github.valentinaebi.capybara.values.NumericValue
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.ValuesCreator
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KBoolSort
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.analysis.Frame


const val TERMINATOR_ADAPTER_INSN_OPCODE = -63

sealed interface BasicBlockTerminator {
    fun resolve(resolver: Map<BasicBlock, BasicBlock>)
    fun fullDescr(): String
}

data class ReturnTerminator(val mustPopValue: Boolean) : BasicBlockTerminator {
    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) = Unit
    override fun toString(): String = "return"
    override fun fullDescr(): String = if (mustPopValue) "nonvoid-return" else "void-return"
}

data object ThrowTerminator : BasicBlockTerminator {
    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) = Unit
    override fun toString(): String = "throw"
    override fun fullDescr(): String = "throw"
}

abstract class SingleSuccessorTerminator(successorBlock: BasicBlock) : BasicBlockTerminator {

    var successor: BasicBlock = successorBlock
        private set

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        successor = resolver[successor]!!
    }
}

class GotoTerminator(successorBlock: BasicBlock) : SingleSuccessorTerminator(successorBlock) {
    override fun toString(): String = "goto $successor"
    override fun fullDescr(): String = "goto $successor"
}

class IteTerminator(
    val cond: OperandStackPredicate,
    successorBlockIfTrue: BasicBlock,
    successorBlockIfFalse: BasicBlock
) : BasicBlockTerminator {

    var successorIfTrue = successorBlockIfTrue
        private set
    var successorIfFalse = successorBlockIfFalse
        private set

    override fun toString(): String = "$successorIfTrue or $successorIfFalse"
    override fun fullDescr(): String = "if $cond then $successorIfTrue else $successorIfFalse"

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        successorIfTrue = resolver[successorIfTrue]!!
        successorIfFalse = resolver[successorIfFalse]!!
    }
}

class TableSwitchTerminator(
    val minKey: Int,
    casesBlocks: List<BasicBlock>,
    defaultBlock: BasicBlock
) : BasicBlockTerminator {

    var cases: List<BasicBlock> = casesBlocks
        private set
    var default: BasicBlock = defaultBlock
        private set

    override fun toString(): String = "tableswitch"
    override fun fullDescr(): String {
        val sb = StringBuilder()
        sb.append("tableswitch\n")
        for ((idx, block) in cases.withIndex()) {
            val key = minKey + idx
            sb.append(" ").append(key).append(" -> ").append(block).append("\n")
        }
        sb.append(" default -> ").append(default).append("\n")
        return sb.toString()
    }

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        cases = cases.map { resolver[it]!! }
        default = resolver[default]!!
    }
}

class LookupSwitchTerminator(
    casesBlocks: Map<Any, BasicBlock>,
    defaultBlock: BasicBlock
) : BasicBlockTerminator {

    var cases: Map<Any, BasicBlock> = casesBlocks
        private set
    var default: BasicBlock = defaultBlock
        private set

    override fun toString(): String = "lookupswitch"
    override fun fullDescr(): String {
        val sb = StringBuilder()
        sb.append("lookupswitch\n")
        for ((key, block) in cases) {
            sb.append(" ").append(key).append(" -> ").append(block).append("\n")
        }
        sb.append(" default -> ").append(default).append("\n")
        return sb.toString()
    }

    override fun resolve(resolver: Map<BasicBlock, BasicBlock>) {
        cases = cases.map { (pv, bb) -> pv to resolver[bb]!! }.toMap()
        default = resolver[default]!!
    }
}

class MethodInvocationTerminator(
    val methodId: MethodIdentifier,
    val isDynamicallyDispatched: Boolean,
    successorBlock: BasicBlock
) : SingleSuccessorTerminator(successorBlock) {

    override fun toString(): String = "invoke $methodId"
    override fun fullDescr(): String =
        "invoke $methodId (${if (isDynamicallyDispatched) "dynamic" else "static"}) ; goto $successor"
}

sealed class AssertionTerminator(successorBlock: BasicBlock) : SingleSuccessorTerminator(successorBlock) {

    abstract val check: Check

    abstract fun mkCorrectnessFormula(
        frame: Frame<ProgramValue>,
        valuesCreator: ValuesCreator,
        ctx: KContext
    ): KExpr<KBoolSort>

    override fun toString(): String = "assertion $check, goto $successor"

}

class AssertNonNullTerminator(
    basicBlock: BasicBlock,
    private val stackDepth: Int,
    override val check: Check
) : AssertionTerminator(basicBlock) {

    override fun mkCorrectnessFormula(
        frame: Frame<ProgramValue>,
        valuesCreator: ValuesCreator,
        ctx: KContext
    ): KExpr<KBoolSort> {
        val value = frame.getStackAtDepth(stackDepth)
        return ctx.mkNot(valuesCreator.areEqualFormula(value, valuesCreator.nullValue))
    }

    override fun fullDescr(): String = "stack[depth=$stackDepth] non-nullness check ; goto $successor ($check)"

}

class AssertInBoundsTerminator(
    basicBlock: BasicBlock,
    private val isStore: Boolean
) : AssertionTerminator(basicBlock) {

    override val check: Check = Check.ARRAY_INDEX_OUT

    override fun mkCorrectnessFormula(
        frame: Frame<ProgramValue>,
        valuesCreator: ValuesCreator,
        ctx: KContext
    ): KExpr<KBoolSort> = with(valuesCreator) {
        val idxStackDepth = if (isStore) 1 else 0
        val array = frame.getStackAtDepth(idxStackDepth + 1).ref()
        val idx = frame.getStackAtDepth(idxStackDepth).int32()
        return ctx.mkAnd(
            lessThanOrEqualToFormula(zero_int, idx),
            lessThanFormula(idx, arrayLen(array))
        )
    }

    override fun fullDescr(): String =
        "array ${if (isStore) "store" else "load"} index check ; goto $successor ($check)"
}

private fun Frame<ProgramValue>.getStackAtDepth(depth: Int): ProgramValue {
    val idxOfTopStackElem = stackSize - 1
    return getStack(idxOfTopStackElem - depth)
}

class AssertValidArrayLengthTerminator(successor: BasicBlock, private val nDimensions: Int) :
    AssertionTerminator(successor) {

    override val check: Check = Check.NEG_ARRAY_LEN

    override fun mkCorrectnessFormula(
        frame: Frame<ProgramValue>,
        valuesCreator: ValuesCreator,
        ctx: KContext
    ): KExpr<KBoolSort> {
        val lengths = (0..<nDimensions).map { frame.getStackAtDepth(it).int32() }
        return ctx.mkAnd(lengths.map { ctx.mkNot(valuesCreator.isLessThanZeroFormula(it)) })
    }

    override fun fullDescr(): String = "array length non-negativity check ; goto $successor ($check)"
}

class AssertValidDivisorTerminator(successor: BasicBlock) : AssertionTerminator(successor) {

    override val check: Check = Check.DIV_BY_ZERO

    override fun mkCorrectnessFormula(
        frame: Frame<ProgramValue>,
        valuesCreator: ValuesCreator,
        ctx: KContext
    ): KExpr<KBoolSort> {
        val divisor = frame.getStackAtDepth(0)
        return ctx.mkNot(valuesCreator.isZeroFormula(divisor as NumericValue<*>))
    }

    override fun fullDescr(): String = "divisor non-nullity check ; goto $successor ($check)"
}

class TerminatorInsnAdapter(val terminator: SingleSuccessorTerminator) :
    AbstractInsnNode(TERMINATOR_ADAPTER_INSN_OPCODE) {

    val successor: BasicBlock get() = terminator.successor

    override fun getType(): Int {
        throw UnsupportedOperationException()
    }

    override fun accept(methodVisitor: MethodVisitor?) {
        throw UnsupportedOperationException()
    }

    override fun clone(clonedLabels: Map<LabelNode?, LabelNode?>?): AbstractInsnNode? {
        throw UnsupportedOperationException()
    }
}
