package com.github.valentinaebi.capybara

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Interpreter

class SymbolicInterpreter : Interpreter<ProgramValue>(API_LEVEL) {

    override fun newValue(type: Type?): ProgramValue {
        TODO("Not yet implemented")
    }

    override fun newOperation(insn: AbstractInsnNode?): ProgramValue {
        TODO("Not yet implemented")
    }

    override fun copyOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue {
        TODO("Not yet implemented")
    }

    override fun unaryOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue {
        TODO("Not yet implemented")
    }

    override fun binaryOperation(insn: AbstractInsnNode?, value1: ProgramValue?, value2: ProgramValue?): ProgramValue {
        TODO("Not yet implemented")
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode?,
        value1: ProgramValue?,
        value2: ProgramValue?,
        value3: ProgramValue?
    ): ProgramValue {
        TODO("Not yet implemented")
    }

    override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out ProgramValue>?): ProgramValue {
        TODO("Not yet implemented")
    }

    override fun returnOperation(insn: AbstractInsnNode?, value: ProgramValue?, expected: ProgramValue?) {
        TODO("Not yet implemented")
    }

    override fun merge(value1: ProgramValue?, value2: ProgramValue?): ProgramValue {
        TODO("Not yet implemented")
    }
}