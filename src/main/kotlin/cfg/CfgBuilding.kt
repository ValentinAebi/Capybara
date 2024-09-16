@file:OptIn(ExperimentalContracts::class)

package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.UNKNOWN_LINE_NUMBER
import com.github.valentinaebi.capybara.cfg.BinaryOperandStackPredicate.Equal
import com.github.valentinaebi.capybara.cfg.BinaryOperandStackPredicate.GreaterThan
import com.github.valentinaebi.capybara.cfg.BinaryOperandStackPredicate.LessThan
import com.github.valentinaebi.capybara.cfg.UnaryOperandStackPredicate.GreaterThanZero
import com.github.valentinaebi.capybara.cfg.UnaryOperandStackPredicate.IsNull
import com.github.valentinaebi.capybara.cfg.UnaryOperandStackPredicate.IsZero
import com.github.valentinaebi.capybara.cfg.UnaryOperandStackPredicate.LessThanZero
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.util.Printer
import kotlin.collections.contains
import kotlin.collections.get
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


// FIXME always iterate on the whole list of instructions instead of using next


fun buildCfg(instructions: List<AbstractInsnNode>, tryCatchBlocks: List<TryCatchBlockNode>): Cfg {

    if (instructions.isEmpty()) {
        return Cfg(emptyList(), emptyList())
    }

    val (tryStartLabels, tryEndLabels, tryHandlerLabels) = collectTryCatchLabels(tryCatchBlocks)

    // 1st pass: collect target labels
    val jumpTargetLabels =
        instructions.filterIsInstance<JumpInsnNode>()
            .map { it.label }
            .toSet()

    // 2nd pass:
    //  - set placeholder basic block for every initial instruction
    //  - build try blocks hierarchy
    //  - collect line numbers
    val (blockStartingAtInsn, tryBlockToParent, labelsToLineNumbers) =
        findBlocksAndLinesAndBuildTryHierarchy(
            instructions,
            jumpTargetLabels,
            tryStartLabels,
            tryEndLabels,
            tryHandlerLabels
        )

    // 3rd pass: collect labels identifying each basic block
    val labelsToBasicBlocks = collectLabelsPrecedingBasicBlockStarts(
        instructions,
        blockStartingAtInsn
    )

    // Build unresolved catches, lazily evaluated in topological order
    val catches = linkedMapOf<TryCatchBlockNode, Catch>()
    tryCatchBlocks.forEach {
        computeCatchIfAbsent(it, catches, tryBlockToParent, labelsToBasicBlocks)
    }

    // 4th pass: build actual basic blocks
    val basicBlocks = buildBasicBlocks(
        instructions,
        blockStartingAtInsn,
        catches,
        labelsToBasicBlocks,
        tryStartLabels,
        tryEndLabels,
        labelsToLineNumbers
    )

    // Resolve links to basic blocks
    basicBlocks.values.forEach { it.terminator.resolve(basicBlocks) }
    catches.values.forEach { it.resolve(basicBlocks) }

    return Cfg(basicBlocks.values.toList(), catches.values.toList())
}

private fun collectTryCatchLabels(
    tryCatchBlocks: List<TryCatchBlockNode>
): Triple<Map<Label, List<TryCatchBlockNode>>, Map<Label, List<TryCatchBlockNode>>, Set<Label>> {
    val tryStartLabels = mutableMapOf<Label, MutableList<TryCatchBlockNode>>()
    val tryEndLabels = mutableMapOf<Label, MutableList<TryCatchBlockNode>>()
    val tryHandlerLabels = mutableSetOf<Label>()
    for (tryCatch in tryCatchBlocks) {
        tryStartLabels.getOrPut(tryCatch.start.label) { mutableListOf() }.add(tryCatch)
        tryEndLabels.getOrPut(tryCatch.end.label) { mutableListOf() }.add(tryCatch)
        tryHandlerLabels.add(tryCatch.handler.label)
    }
    return Triple(tryStartLabels, tryEndLabels, tryHandlerLabels)
}

private fun findBlocksAndLinesAndBuildTryHierarchy(
    instructions: List<AbstractInsnNode>,
    jumpTargetLabels: Set<LabelNode>,
    tryStartLabels: Map<Label, List<TryCatchBlockNode>>,
    tryEndLabels: Map<Label, List<TryCatchBlockNode>>,
    tryHandlerLabels: Set<Label>
): Triple<Array<BasicBlock?>, Map<TryCatchBlockNode, TryCatchBlockNode?>, Map<Label, Int>> {
    val blockStartingAtInsn = arrayOfNulls<BasicBlock>(instructions.size)
    val tryBlockToParent = mutableMapOf<TryCatchBlockNode, TryCatchBlockNode?>()
    val labelsToLineNumbers = mutableMapOf<Label, Int>()
    var currentTryBlock: TryCatchBlockNode? = null
    blockStartingAtInsn[0] = newPlaceholderBlock()
    for ((currInsnIdx, currInsn) in instructions.withIndex()) {
        val opcode = currInsn.opcode
        if (opcode == Opcodes.RET || opcode == Opcodes.JSR) {
            throw AnalyzerException(currInsn, "unsupported Java <7 instruction (JSR, RET)")
        }
        if (currInsn is LabelNode) {
            tryStartLabels[currInsn.label]?.let {
                for (tryB in it) {
                    tryBlockToParent[tryB] = currentTryBlock
                    currentTryBlock = tryB
                }
            }
            tryEndLabels[currInsn.label]?.let {
                for (tryB in it.reversed()) {
                    assert(tryB == currentTryBlock)
                    currentTryBlock = tryBlockToParent[currentTryBlock]
                }
            }
        } else if (currInsn is LineNumberNode) {
            labelsToLineNumbers[currInsn.start.label] = currInsn.line
        } else if (currInsn is AssertionTerminator){
            blockStartingAtInsn[currInsnIdx + 1] = currInsn.successor
        } else if (currInsnIdx < instructions.size - 1 && blockStartingAtInsn[currInsnIdx + 1] == null && (
                    currInsn.type == AbstractInsnNode.JUMP_INSN
                            || isSwitchOpcode(opcode)
                            || isReturnOpcode(opcode)
                            || opcode == Opcodes.ATHROW
                    )
        ) {
            blockStartingAtInsn[currInsnIdx + 1] = newPlaceholderBlock()
        }
        if (
            currInsn in jumpTargetLabels
            || isTryStartInsn(currInsn, tryStartLabels)
            || isHandlerStartInsn(currInsn, tryHandlerLabels)
        ) {
            val predIdx = goBackToLastConcreteInsn(currInsn, currInsnIdx)
            if (predIdx != null && blockStartingAtInsn[predIdx + 1] == null) {
                blockStartingAtInsn[predIdx + 1] = newPlaceholderBlock()
            }
        }
    }
    return Triple(blockStartingAtInsn, tryBlockToParent, labelsToLineNumbers)
}

private fun collectLabelsPrecedingBasicBlockStarts(
    instructions: List<AbstractInsnNode>,
    blockStartingAtInsn: Array<BasicBlock?>
): Map<Label, BasicBlock> {
    val labelsToBasicBlock = mutableMapOf<Label, BasicBlock>()
    var currBasicBlock: BasicBlock? = null
    for ((currInsnIdx, currInsn) in instructions.withIndex()) {
        blockStartingAtInsn[currInsnIdx]?.let {
            currBasicBlock = it
        }
        if (isConcreteInsn(currInsn)) {
            currBasicBlock = null
        } else if (currBasicBlock != null && currInsn is LabelNode) {
            labelsToBasicBlock[currInsn.label] = currBasicBlock
        }
    }
    return labelsToBasicBlock
}

private fun buildBasicBlocks(
    instructions: List<AbstractInsnNode>,
    blockStartingAtInsn: Array<BasicBlock?>,
    catches: LinkedHashMap<TryCatchBlockNode, Catch>,
    labelsToBasicBlocks: Map<Label, BasicBlock>,
    tryStartLabels: Map<Label, List<TryCatchBlockNode>>,
    tryEndLabels: Map<Label, List<TryCatchBlockNode>>,
    labelsToLineNumbers: Map<Label, Int>
): Map<BasicBlock, BasicBlock> {

    val basicBlocks = mutableMapOf<BasicBlock, BasicBlock>()
    val iter = instructions.iterator()
    var currInsn: AbstractInsnNode? = iter.next()
    var currInsnIdx = 0
    var currCatch: Catch? = null
    var currLineNumber = UNKNOWN_LINE_NUMBER

    while (currInsn != null) {

        val blockInsns = linkedMapOf<AbstractInsnNode, Int>()
        val indexOfFirstInsnInCurrBlock = currInsnIdx
        var indexOfLastInsnAddedToBlock = -1
        var catchForCurrentBlock: Catch? = null
        while (currInsn != null && (currInsnIdx == indexOfFirstInsnInCurrBlock || blockStartingAtInsn[currInsnIdx] == null)) {
            if (currInsn is LabelNode) {
                labelsToLineNumbers[currInsn.label]?.let {
                    currLineNumber = it
                }
                tryStartLabels[currInsn.label]?.let {
                    currCatch = catches[it.last()]
                }
                tryEndLabels[currInsn.label]?.let {
                    assert(currCatch == catches[it.last()])
                    currCatch = catches[it.first()]?.parentCatch
                }
            } else if (isConcreteInsn(currInsn)) {
                if (blockInsns.isEmpty()) {
                    catchForCurrentBlock = currCatch
                }
                blockInsns.put(currInsn, currLineNumber)
                indexOfLastInsnAddedToBlock = currInsnIdx
            }
            currInsn = if (iter.hasNext()) iter.next() else null
            currInsnIdx += 1
        }

        if (blockInsns.isNotEmpty()) {
            val lastInsnInBlock = blockInsns.lastEntry().key
            val terminator = computeTerminator(
                lastInsnInBlock,
                indexOfLastInsnAddedToBlock,
                labelsToBasicBlocks,
                blockStartingAtInsn
            )
            if (lastInsnInBlock.opcode == Opcodes.GOTO || terminator !is SingleSuccessorTerminator) {
                blockInsns.remove(lastInsnInBlock)
            }
            val placeholderBlock = blockStartingAtInsn[indexOfFirstInsnInCurrBlock]!!
            basicBlocks[placeholderBlock] =
                BasicBlock(blockInsns, terminator, catchForCurrentBlock, basicBlocks.size)
        }
    }
    return basicBlocks
}

private fun computeTerminator(
    lastInsnInBlock: AbstractInsnNode,
    idxOfLastInsnInBlock: Int,
    labelsToBasicBlocks: Map<Label, BasicBlock>,
    blockStartingAtInsn: Array<BasicBlock?>
): BasicBlockTerminator {

    fun nextBasicBlock(): BasicBlock {
        var currInsnIdx = idxOfLastInsnInBlock + 1
        while (true) {
            val maybeBlock = blockStartingAtInsn[currInsnIdx]
            if (maybeBlock != null) {
                return maybeBlock
            }
            currInsnIdx += 1
        }
    }

    val opcode = lastInsnInBlock.opcode
    when {

        isReturnOpcode(opcode) -> return ReturnTerminator(opcode != Opcodes.RETURN)

        opcode == Opcodes.ATHROW -> return ThrowTerminator

        lastInsnInBlock is JumpInsnNode -> {
            val labelTarget = labelsToBasicBlocks[lastInsnInBlock.label.label]!!
            return when (lastInsnInBlock.opcode) {
                Opcodes.IFEQ -> IteTerminator(IsZero, labelTarget, nextBasicBlock())
                Opcodes.IFNE -> IteTerminator(IsZero, nextBasicBlock(), labelTarget)
                Opcodes.IFLT -> IteTerminator(LessThanZero, labelTarget, nextBasicBlock())
                Opcodes.IFGE -> IteTerminator(LessThanZero, nextBasicBlock(), labelTarget)
                Opcodes.IFGT -> IteTerminator(GreaterThanZero, labelTarget, nextBasicBlock())
                Opcodes.IFLE -> IteTerminator(GreaterThanZero, nextBasicBlock(), labelTarget)
                Opcodes.IF_ICMPEQ, Opcodes.IF_ACMPEQ -> IteTerminator(Equal, labelTarget, nextBasicBlock())
                Opcodes.IF_ICMPNE, Opcodes.IF_ACMPNE -> IteTerminator(Equal, nextBasicBlock(), labelTarget)
                Opcodes.IF_ICMPLT -> IteTerminator(LessThan, labelTarget, nextBasicBlock())
                Opcodes.IF_ICMPGE -> IteTerminator(LessThan, nextBasicBlock(), labelTarget)
                Opcodes.IF_ICMPGT -> IteTerminator(GreaterThan, labelTarget, nextBasicBlock())
                Opcodes.IF_ICMPLE -> IteTerminator(GreaterThan, nextBasicBlock(), labelTarget)
                Opcodes.GOTO -> SingleSuccessorTerminator(labelsToBasicBlocks[lastInsnInBlock.label.label]!!)
                Opcodes.IFNULL -> IteTerminator(IsNull, labelTarget, nextBasicBlock())
                Opcodes.IFNONNULL -> IteTerminator(IsNull, nextBasicBlock(), labelTarget)
                else -> throw AssertionError("unexpected opcode: ${Printer.OPCODES[opcode]}")
            }
        }

        lastInsnInBlock is TableSwitchInsnNode -> {
            val cases = lastInsnInBlock.labels.map { labelsToBasicBlocks[it.label]!! }
            val default = labelsToBasicBlocks[lastInsnInBlock.dflt.label]!!
            return TableSwitchTerminator(lastInsnInBlock.min, cases, default)
        }

        lastInsnInBlock is LookupSwitchInsnNode -> {
            val keys = lastInsnInBlock.keys
            val labels = lastInsnInBlock.labels
            val cases = mutableMapOf<Any, BasicBlock>()
            for (i in keys.indices) {
                cases[keys[i]] = labelsToBasicBlocks[labels[i].label]!!
            }
            val default = labelsToBasicBlocks[lastInsnInBlock.dflt.label]!!
            return LookupSwitchTerminator(cases, default)
        }

        lastInsnInBlock is AssertionTerminator -> {
            return lastInsnInBlock
        }

        else -> return SingleSuccessorTerminator(nextBasicBlock())
    }
}

fun computeCatchIfAbsent(
    tryCatchBlockNode: TryCatchBlockNode,
    catches: MutableMap<TryCatchBlockNode, Catch>,
    tryBlockToParent: Map<TryCatchBlockNode, TryCatchBlockNode?>,
    labelsToBasicBlock: Map<Label, BasicBlock>
): Catch {
    catches[tryCatchBlockNode]?.let {
        return@computeCatchIfAbsent it
    }
    val childrenList = mutableListOf<Catch>()
    val maybeParentTryCatchBlock = tryBlockToParent[tryCatchBlockNode]
    val maybeParentCatch = maybeParentTryCatchBlock?.let {
        computeCatchIfAbsent(it, catches, tryBlockToParent, labelsToBasicBlock)
    }
    val catch = Catch(
        tryCatchBlockNode.type,
        labelsToBasicBlock[tryCatchBlockNode.handler.label]!!,
        maybeParentCatch,
        childrenList,
        catches.size
    )
    catches[tryCatchBlockNode] = catch
    maybeParentCatch?.let { (it.childrenCatches as MutableList<Catch>).add(catch) }
    return catch
}

private fun goBackToLastConcreteInsn(insn: AbstractInsnNode, insnIdx: Int): Int? {
    var currInsn: AbstractInsnNode? = insn.previous
    var currInsnIdx = insnIdx - 1
    while (currInsn != null) {
        if (isConcreteInsn(currInsn)) {
            return currInsnIdx
        }
        currInsn = currInsn.previous
        currInsnIdx -= 1
    }
    return null
}

private fun isConcreteInsn(currInsn: AbstractInsnNode): Boolean = currInsn.opcode != -1

private fun newPlaceholderBlock() = BasicBlock(linkedMapOf(), ReturnTerminator(false), null, -1)

private fun isHandlerStartInsn(
    insn: AbstractInsnNode,
    tryHandlerLabels: Set<Label>
): Boolean {
    contract { returns(true) implies (insn is LabelNode) }
    return insn is LabelNode && insn.label in tryHandlerLabels
}

private fun isTryStartInsn(
    insn: AbstractInsnNode,
    tryStartLabels: Map<Label, Any>
): Boolean {
    contract { returns(true) implies (insn is LabelNode) }
    return insn is LabelNode && insn.label in tryStartLabels
}

private fun isTryEndInsn(
    insn: AbstractInsnNode,
    tryEndLabels: Map<Label, Any>
): Boolean {
    contract { returns(true) implies (insn is LabelNode) }
    return insn is LabelNode && insn.label in tryEndLabels
}

private fun isReturnOpcode(opcode: Int): Boolean = Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN

private fun isSwitchOpcode(opcode: Int): Boolean = opcode == Opcodes.TABLESWITCH || opcode == Opcodes.LOOKUPSWITCH
