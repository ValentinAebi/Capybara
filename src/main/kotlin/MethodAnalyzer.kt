package com.github.valentinaebi.capybara

import com.github.valentinaebi.capybara.cfg.BBTerminator
import com.github.valentinaebi.capybara.cfg.BasicBlock
import com.github.valentinaebi.capybara.cfg.Catch
import com.github.valentinaebi.capybara.cfg.Method
import com.github.valentinaebi.capybara.cfg.ReturnTerminator
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.tree.analysis.AnalyzerException
import org.objectweb.asm.util.Printer


class MethodAnalyzer(
    private val ownerClassInternalName: String,
    methodNode: MethodNode
) : MethodVisitor(API_LEVEL, methodNode) {

    private val methodNode: MethodNode get() = mv as MethodNode

    override fun visitEnd() {
        val instructions = methodNode.instructions.toArray()
        println("Method ${methodNode.name}:")
        for (insn in instructions) {
            val opcode = insn.opcode
            if (opcode >= 0) {
                println(Printer.OPCODES[opcode])
            }
        }
        println()
    }

    private fun analyzeMethod(): Method {

        val instructions = methodNode.instructions.toArray()
        val tryCatchBlocks = methodNode.tryCatchBlocks

        if (instructions.isEmpty()) {
            return Method(emptyList(), emptyList())
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
            findBlocksAndLinesAndBuildCatchesHierarchy(
                instructions,
                jumpTargetLabels,
                tryStartLabels,
                tryEndLabels,
                tryHandlerLabels
            )

        // 3rd pass: collect labels identifying each basic block
        val labelsToBasicBlock = collectLabelsPrecedingBasicBlockStarts(
            instructions.first(),
            blockStartingAtInsn
        )

        // Build unresolved catches, lazily evaluated in topological order
        val catches = linkedMapOf<TryCatchBlockNode, Catch>()
        tryCatchBlocks.forEach {
            computeCatchIfAbsent(it, catches, tryBlockToParent, labelsToBasicBlock)
        }

        // 4th pass: build actual basic blocks
        val basicBlocks = buildBasicBlocks(
            instructions.first(),
            blockStartingAtInsn,
            tryStartLabels,
            catches,
            tryEndLabels
        )

        // Resolve links to basic blocks
        basicBlocks.values.forEach { it.terminator.resolve(basicBlocks) }
        catches.values.forEach { it.resolve(basicBlocks) }

        return Method(basicBlocks.values.toList(), catches.values.toList())
    }

    private fun buildBasicBlocks(
        firstInstruction: AbstractInsnNode,
        blockStartingAtInsn: Array<BasicBlock?>,
        tryStartLabels: MutableMap<Label, TryCatchBlockNode>,
        catches: LinkedHashMap<TryCatchBlockNode, Catch>,
        tryEndLabels: MutableMap<Label, TryCatchBlockNode>
    ): MutableMap<BasicBlock, BasicBlock> {

        val basicBlocks = mutableMapOf<BasicBlock, BasicBlock>()

        var currInsn: AbstractInsnNode? = firstInstruction
        var currInsnIdx = 0
        var currCatch: Catch? = null

        while (currInsn != null) {

            val blockInsns = mutableListOf<AbstractInsnNode>()
            val indexOfFirstInsnInCurrBlock = currInsnIdx
            while (currInsn != null && (currInsnIdx == indexOfFirstInsnInCurrBlock || blockStartingAtInsn[currInsnIdx] == null)) {
                if (currInsn is LabelNode) {
                    tryStartLabels[currInsn.label]?.let {
                        currCatch = catches[it]
                    }
                    tryEndLabels[currInsn.label]?.let {
                        assert(currCatch == catches[it])
                        currCatch = currCatch?.parentCatch
                    }
                } else if (isConcreteInsn(currInsn)) {
                    // TODO also add line numbers
                    blockInsns.add(currInsn)
                }
                currInsn = currInsn.next
                currInsnIdx += 1
            }

            if (blockInsns.isNotEmpty()) {
                val terminator: BBTerminator = TODO("compute terminator according to last instruction in list")
                val block = BasicBlock(blockInsns, terminator, currCatch)
                val placeholderBlock = blockStartingAtInsn[indexOfFirstInsnInCurrBlock]!!
                basicBlocks[placeholderBlock] = block
            }
        }
        return basicBlocks
    }

    private fun collectTryCatchLabels(
        tryCatchBlocks: List<TryCatchBlockNode>
    ): Triple<MutableMap<Label, TryCatchBlockNode>, MutableMap<Label, TryCatchBlockNode>, MutableMap<Label, TryCatchBlockNode>> {
        val tryStartLabels = mutableMapOf<Label, TryCatchBlockNode>()
        val tryEndLabels = mutableMapOf<Label, TryCatchBlockNode>()
        val tryHandlerLabels = mutableMapOf<Label, TryCatchBlockNode>()
        for (tryCatch in tryCatchBlocks) {
            tryStartLabels[tryCatch.start.label] = tryCatch
            tryEndLabels[tryCatch.end.label] = tryCatch
            tryHandlerLabels[tryCatch.handler.label] = tryCatch
        }
        return Triple(tryStartLabels, tryEndLabels, tryHandlerLabels)
    }

    private fun findBlocksAndLinesAndBuildCatchesHierarchy(
        instructions: Array<AbstractInsnNode>,
        jumpTargetLabels: Set<LabelNode>,
        tryStartLabels: MutableMap<Label, TryCatchBlockNode>,
        tryEndLabels: MutableMap<Label, TryCatchBlockNode>,
        tryHandlerLabels: MutableMap<Label, TryCatchBlockNode>
    ): Triple<Array<BasicBlock?>, Map<TryCatchBlockNode, TryCatchBlockNode?>, Map<Label, Int>> {
        val blockStartingAtInsn = Array<BasicBlock?>(instructions.size) { null }
        val tryBlockToParent = mutableMapOf<TryCatchBlockNode, TryCatchBlockNode?>()
        val labelsToLineNumbers = mutableMapOf<Label, Int>()
        var currInsn: AbstractInsnNode? = instructions[0]
        var currInsnIdx = 0
        var currentTryBlock: TryCatchBlockNode? = null
        blockStartingAtInsn[0] = newPlaceholderBlock()
        while (currInsn != null) {
            val opcode = currInsn.opcode
            if (opcode == Opcodes.RET || opcode == Opcodes.JSR) {
                throw AnalyzerException(currInsn, "unsupported Java <7 instruction")
            }
            val isLabelNode = currInsn is LabelNode
            if (isLabelNode) {
                tryStartLabels[currInsn.label]?.let {
                    tryBlockToParent[it] = currentTryBlock
                    currentTryBlock = it
                }
                tryEndLabels[currInsn.label]?.let {
                    assert(it == currentTryBlock)
                    currentTryBlock = tryBlockToParent[currentTryBlock]
                }
            } else if (currInsn is LineNumberNode) {
                labelsToLineNumbers[currInsn.start.label] = currInsn.line
            }
            if (currInsnIdx < instructions.size - 1 && blockStartingAtInsn[currInsnIdx] == null && (
                        currInsn.type == AbstractInsnNode.JUMP_INSN
                                || isReturnOpcode(opcode)
                                || opcode == Opcodes.ATHROW
                        )
            ) {
                blockStartingAtInsn[currInsnIdx + 1] = newPlaceholderBlock()
            }
            if (
                isJumpTarget(currInsn, jumpTargetLabels)
                || isTryStartInsn(currInsn, tryStartLabels)
                || isHandlerStartInsn(currInsn, tryHandlerLabels)
            ) {
                val predIdx = goBackToLastConcreteInsn(currInsn, currInsnIdx)
                if (predIdx != null && blockStartingAtInsn[predIdx + 1] == null) {
                    blockStartingAtInsn[predIdx + 1] = newPlaceholderBlock()
                }
            }
            currInsn = currInsn.next
            currInsnIdx += 1
        }
        return Triple(blockStartingAtInsn, tryBlockToParent, labelsToLineNumbers)
    }

    private fun collectLabelsPrecedingBasicBlockStarts(
        firstInstruction: AbstractInsnNode,
        blockStartingAtInsn: Array<BasicBlock?>
    ): MutableMap<Label, BasicBlock> {
        val labelsToBasicBlock = mutableMapOf<Label, BasicBlock>()
        var currBasicBlock: BasicBlock? = null
        var currInsn: AbstractInsnNode? = firstInstruction
        var currInsnIdx = 0
        while (currInsn != null) {
            blockStartingAtInsn[currInsnIdx]?.let {
                currBasicBlock = it
            }
            if (isConcreteInsn(currInsn)) {
                currBasicBlock = null
            } else if (currBasicBlock != null && currInsn is LabelNode) {
                labelsToBasicBlock[currInsn.label] = currBasicBlock
            }
            currInsn = currInsn.next
            currInsnIdx += 1
        }
        return labelsToBasicBlock
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
            ReferenceType(tryCatchBlockNode.type),
            labelsToBasicBlock[tryCatchBlockNode.handler.label]!!,
            maybeParentCatch,
            childrenList
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

    private fun isConcreteInsn(currInsn: AbstractInsnNode): Boolean =
        currInsn !is LabelNode && currInsn !is LineNumberNode

    private fun newPlaceholderBlock() = BasicBlock(emptyList(), ReturnTerminator, null)

    private fun isJumpTarget(
        insn: AbstractInsnNode,
        jumpTargetLabels: Set<LabelNode>
    ): Boolean = insn is JumpInsnNode && insn.label in jumpTargetLabels

    private fun isHandlerStartInsn(
        insn: AbstractInsnNode,
        tryHandlerLabels: MutableMap<Label, TryCatchBlockNode>
    ): Boolean = insn is LabelNode && insn.label in tryHandlerLabels

    private fun isTryStartInsn(
        insn: AbstractInsnNode,
        tryStartLabels: MutableMap<Label, TryCatchBlockNode>
    ): Boolean = insn is LabelNode && insn.label in tryStartLabels

    private fun isTryEndInsn(
        insn: AbstractInsnNode,
        tryEndLabels: MutableMap<Label, TryCatchBlockNode>
    ): Boolean = insn is LabelNode && insn.label in tryEndLabels

    private fun isReturnOpcode(opcode: Int): Boolean = Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN

}
