package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.ProgramValue
import com.github.valentinaebi.capybara.SymbolicInterpreter
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Frame

class BasicBlock(
    val insnList: List<AbstractInsnNode>,
    val terminator: BBTerminator,
    val lastCatch: Catch?
) {

    fun simulate(frame: Frame<ProgramValue>, interpreter: SymbolicInterpreter) {
        for (insn in insnList) {
            frame.execute(insn, interpreter)
        }
    }

}
