package com.github.valentinaebi.capybara.execution

import com.github.valentinaebi.capybara.API_LEVEL
import com.github.valentinaebi.capybara.Formula
import com.github.valentinaebi.capybara.IsNull
import com.github.valentinaebi.capybara.types.ReferenceType
import com.github.valentinaebi.capybara.values.ConcreteDoubleValue
import com.github.valentinaebi.capybara.values.ConcreteFloatValue
import com.github.valentinaebi.capybara.values.ConcreteInt32BitsValue
import com.github.valentinaebi.capybara.values.ConcreteLongValue
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.RawValueType
import com.github.valentinaebi.capybara.values.RawValueType.Reference
import com.github.valentinaebi.capybara.values.SecondBytePlaceholder
import com.github.valentinaebi.capybara.values.SymbolicValue
import com.github.valentinaebi.capybara.values.five_int
import com.github.valentinaebi.capybara.values.four_int
import com.github.valentinaebi.capybara.values.minusOne_int
import com.github.valentinaebi.capybara.values.one_double
import com.github.valentinaebi.capybara.values.one_float
import com.github.valentinaebi.capybara.values.one_int
import com.github.valentinaebi.capybara.values.one_long
import com.github.valentinaebi.capybara.values.three_int
import com.github.valentinaebi.capybara.values.two_float
import com.github.valentinaebi.capybara.values.two_int
import com.github.valentinaebi.capybara.values.zero_double
import com.github.valentinaebi.capybara.values.zero_float
import com.github.valentinaebi.capybara.values.zero_int
import com.github.valentinaebi.capybara.values.zero_long
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.util.Printer

class SymbolicInterpreter : Interpreter<ProgramValue>(API_LEVEL) {

    val raisedException: ReferenceType? get() = _raisedException
    private var _raisedException: ReferenceType? = null

    private var newConstraints = mutableListOf<Formula>()

    private val knownAttributes = mutableMapOf<Pair<ProgramValue, String>, ProgramValue>()
    private val knownStrings = mutableMapOf<ProgramValue, String>()

    fun getAndResetNewConstraints(): List<Formula> {
        val result = newConstraints
        newConstraints = mutableListOf()
        return result
    }

    private fun addNewConstraint(constraint: Formula) {
        newConstraints.add(constraint)
    }

    override fun newValue(type: Type?): ProgramValue {
        return if (type == null) {
            SecondBytePlaceholder
        } else {
            SymbolicValue(RawValueType.fromAsmSort(type.sort))
        }
    }

    override fun newOperation(insn: AbstractInsnNode?): ProgramValue {
        val opcode = insn!!.opcode
        return when (opcode) {
            Opcodes.ACONST_NULL -> {
                val nullValue = SymbolicValue(Reference)
                addNewConstraint(IsNull(nullValue))
                nullValue
            }

            Opcodes.ICONST_M1 -> minusOne_int
            Opcodes.ICONST_0 -> zero_int
            Opcodes.ICONST_1 -> one_int
            Opcodes.ICONST_2 -> two_int
            Opcodes.ICONST_3 -> three_int
            Opcodes.ICONST_4 -> four_int
            Opcodes.ICONST_5 -> five_int
            Opcodes.LCONST_0 -> zero_long
            Opcodes.LCONST_1 -> one_long
            Opcodes.FCONST_0 -> zero_float
            Opcodes.FCONST_1 -> one_float
            Opcodes.FCONST_2 -> two_float
            Opcodes.DCONST_0 -> zero_double
            Opcodes.DCONST_1 -> one_double
            Opcodes.BIPUSH -> ConcreteInt32BitsValue((insn as IntInsnNode).operand)
            Opcodes.SIPUSH -> ConcreteInt32BitsValue((insn as IntInsnNode).operand)
            Opcodes.LDC -> {
                val cst = (insn as LdcInsnNode).cst
                when (cst) {
                    is Int -> ConcreteInt32BitsValue(cst)
                    is Long -> ConcreteLongValue(cst)
                    is Float -> ConcreteFloatValue(cst)
                    is Double -> ConcreteDoubleValue(cst)
                    is String -> {
                        val strValue = SymbolicValue(Reference)
                        knownStrings[strValue] = cst
                        strValue
                    }

                    else -> SymbolicValue(Reference)
                }
            }

            Opcodes.GETSTATIC -> {
                val descriptor = (insn as FieldInsnNode).desc
                val sort = Type.getType(descriptor).sort
                return SymbolicValue(RawValueType.fromAsmSort(sort))
            }

            Opcodes.NEW -> SymbolicValue(Reference)
            else -> throw AssertionError("unexpected opcode: ${Printer.OPCODES[opcode]}")
        }
    }

    override fun copyOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue = value!!

    override fun unaryOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue {
        /*
        INEG, LNEG, FNEG, DNEG, IINC, I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S,
        TABLESWITCH, LOOKUPSWITCH, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, PUTSTATIC,
        GETFIELD, NEWARRAY, ANEWARRAY, ARRAYLENGTH, ATHROW, CHECKCAST, INSTANCEOF, MONITORENTER, MONITOREXIT
         */
        TODO()
    }

    override fun binaryOperation(insn: AbstractInsnNode?, value1: ProgramValue?, value2: ProgramValue?): ProgramValue {
        assert(value1!!.size == value2!!.size)
        /*
        IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB,
        IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM, ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR,
        IAND, LAND, IOR, LOR, IXOR, LXOR, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, PUTFIELD
         */
        TODO()
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode?,
        value1: ProgramValue?,
        value2: ProgramValue?,
        value3: ProgramValue?
    ): ProgramValue {
        assert(value2!!.size == value3!!.size)
        TODO()
    }

    override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out ProgramValue>?): ProgramValue {
        return placeholder32BitsValue
    }

    override fun returnOperation(insn: AbstractInsnNode?, value: ProgramValue?, expected: ProgramValue?) {
        throw UnsupportedOperationException("returnOperation is not supposed to be called in ${SymbolicInterpreter.javaClass.name}")
    }

    override fun merge(value1: ProgramValue?, value2: ProgramValue?): ProgramValue {
        throw UnsupportedOperationException("merged is not supposed to be called in ${SymbolicInterpreter.javaClass.name}")
    }

    private companion object {
        private val placeholder32BitsValue = SymbolicValue(RawValueType.Placeholder)
    }

}
