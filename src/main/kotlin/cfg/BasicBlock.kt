package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.SymbolicInterpreter
import com.github.valentinaebi.capybara.types.SubtypingRelation
import com.github.valentinaebi.capybara.types.Type
import com.github.valentinaebi.capybara.values.ProgramValue
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Frame

class BasicBlock(
    val insnList: LinkedHashMap<AbstractInsnNode, Int>,
    val terminator: BasicBlockTerminator,
    val surroundingCatch: Catch?
) {

    fun simulateInstructions(
        frame: Frame<ProgramValue>,
        interpreter: SymbolicInterpreter
    ) {
        for ((insn, _) in insnList) {
            frame.execute(insn, interpreter)
            if (interpreter.raisedException != null) {
                break
            }
        }
    }

    private fun findHandler(
        firstCatch: Catch?,
        exceptionType: Type,
        subtypingRelation: SubtypingRelation
    ): BasicBlock? {
        return if (firstCatch == null) null
        else if (exceptionType.isSubtypeOf(firstCatch.handledExceptionType, subtypingRelation)) firstCatch.handler
        else findHandler(firstCatch, exceptionType, subtypingRelation)
    }

}
