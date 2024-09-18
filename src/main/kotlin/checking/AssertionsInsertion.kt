package com.github.valentinaebi.capybara.checking

import com.github.valentinaebi.capybara.cfg.AssertInBoundsTerminator
import com.github.valentinaebi.capybara.cfg.AssertNonNullTerminator
import com.github.valentinaebi.capybara.cfg.AssertValidArrayLengthTerminator
import com.github.valentinaebi.capybara.cfg.BasicBlock
import com.github.valentinaebi.capybara.cfg.IsValidDivisorTerminator
import com.github.valentinaebi.capybara.cfg.ReturnTerminator
import com.github.valentinaebi.capybara.cfg.SingleSuccessorTerminator
import com.github.valentinaebi.capybara.cfg.TerminatorInsnAdapter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode


fun insertAssertions(instructions: Array<AbstractInsnNode>): List<AbstractInsnNode> {
    val augmented = mutableListOf<AbstractInsnNode>()

    fun addTerminatorInsn(terminator: SingleSuccessorTerminator) {
        augmented.add(TerminatorInsnAdapter(terminator))
    }

    for (insn in instructions) {
        when (val opcode = insn.opcode) {

            Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL, Opcodes.INVOKEDYNAMIC -> {
                val argsCnt = Type.getMethodType((insn as MethodInsnNode).desc).argumentCount
                addTerminatorInsn(AssertNonNullTerminator(newPlaceholderBlock(), argsCnt, Check.INVK_NULL_REC))
            }

            Opcodes.GETFIELD, Opcodes.PUTFIELD -> {
                val stackDepth = if (opcode == Opcodes.GETFIELD) 0 else 1
                addTerminatorInsn(AssertNonNullTerminator(newPlaceholderBlock(), stackDepth, Check.FLD_NULL_OWNER))
            }

            in Opcodes.IALOAD..Opcodes.SALOAD -> {
                addTerminatorInsn(AssertNonNullTerminator(newPlaceholderBlock(), 1, Check.INDEXING_NULL_ARRAY))
                addTerminatorInsn(AssertInBoundsTerminator(newPlaceholderBlock(), isStore = false))
            }

            in Opcodes.IASTORE..Opcodes.SASTORE -> {
                addTerminatorInsn(AssertNonNullTerminator(newPlaceholderBlock(), 2, Check.INDEXING_NULL_ARRAY))
                addTerminatorInsn(AssertInBoundsTerminator(newPlaceholderBlock(), isStore = true))
            }

            Opcodes.NEWARRAY, Opcodes.ANEWARRAY, Opcodes.MULTIANEWARRAY -> {
                val nDims = if (insn is MultiANewArrayInsnNode) insn.dims else 1
                addTerminatorInsn(AssertValidArrayLengthTerminator(newPlaceholderBlock(), nDims))
            }

            Opcodes.IDIV, Opcodes.LDIV, Opcodes.IREM, Opcodes.LREM -> {
                addTerminatorInsn(IsValidDivisorTerminator(newPlaceholderBlock()))
            }

        }
        augmented.add(insn)
    }
    return augmented
}

private fun newPlaceholderBlock() = BasicBlock(linkedMapOf(), ReturnTerminator(false), null, -2)
