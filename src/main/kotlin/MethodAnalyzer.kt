package com.github.valentinaebi.capybara

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
        if (instructions.isEmpty()) {
            return Method(emptyList(), emptyList())
        }

        // Collect try/catch labels
        val tryStartLabels = mutableMapOf<Label, TryCatchBlockNode>()
        val tryEndLabels = mutableMapOf<Label, TryCatchBlockNode>()
        val tryHandlerLabels = mutableMapOf<Label, TryCatchBlockNode>()
        for (tryCatch in methodNode.tryCatchBlocks) {
            tryStartLabels[tryCatch.start.label] = tryCatch
            tryEndLabels[tryCatch.end.label] = tryCatch
            tryHandlerLabels[tryCatch.handler.label] = tryCatch
        }

        // 1st pass: collect target labels
        val jumpTargetLabels =
            instructions.filterIsInstance<JumpInsnNode>()
                .map { it.label }
                .toSet()

        // 2nd pass: mark terminal instructions and build try blocks hierarchy
        val isTerminalInsn = Array(instructions.size) { false }
        val tryBlockToParent = mutableMapOf<TryCatchBlockNode, TryCatchBlockNode?>()
        var currInsn: AbstractInsnNode? = instructions[0]
        var currInsnIdx = 0
        var currentTryBlock: TryCatchBlockNode? = null
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
            }
            // An instruction is the last one of its BB if:
            isTerminalInsn[currInsnIdx] = isTerminalInsn[currInsnIdx]
                    // - it is a jump instruction
                    || currInsn.type == AbstractInsnNode.JUMP_INSN
                    // - it is a return instruction
                    || isReturnOpcode(opcode)
                    // - it is a throw instruction
                    || opcode == Opcodes.ATHROW
                    // - it is the last instruction in a try block
                    || (isLabelNode && currInsn.label in tryEndLabels)
            if (currInsnIdx > 0) {
                // An instruction is the first one of its BB if:
                isTerminalInsn[currInsnIdx - 1] = isTerminalInsn[currInsnIdx - 1]
                        // - it is the target of a jump
                        || (currInsn is JumpInsnNode && currInsn.label in jumpTargetLabels)
                        // - it is the first instruction in a try block
                        || (isLabelNode && currInsn.label in tryStartLabels)
                        // - it is the beginning of a handler
                        || (isLabelNode && currInsn.label in tryHandlerLabels)
            }
            currInsn = currInsn.next
            currInsnIdx += 1
        }

        val basicBlocks = mutableMapOf<AbstractInsnNode, BasicBlock>()
        val unresolvedCatches = mutableMapOf<Label, Catch>()
        val placeholderHandler = BasicBlock(emptyList(), ReturnTerminator, null)

        // TODO build unresolved catches
        // TODO build basic blocks
        // TODO resolve catches

    }

    private fun isReturnOpcode(opcode: Int): Boolean = Opcodes.IRETURN <= opcode && opcode <= Opcodes.RETURN

}
