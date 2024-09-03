package com.github.valentinaebi.capybara.execution

import com.github.valentinaebi.capybara.API_LEVEL
import com.github.valentinaebi.capybara.repositories.ArraysRepository
import com.github.valentinaebi.capybara.repositories.OwnedObjectsRepository
import com.github.valentinaebi.capybara.repositories.StringsRepository
import com.github.valentinaebi.capybara.solver.ConstraintsRepository
import com.github.valentinaebi.capybara.types.ReferenceType
import com.github.valentinaebi.capybara.values.AtomicSymbolicValue
import com.github.valentinaebi.capybara.values.ConcreteDoubleValue
import com.github.valentinaebi.capybara.values.ConcreteFloatValue
import com.github.valentinaebi.capybara.values.ConcreteInt32BitsValue
import com.github.valentinaebi.capybara.values.ConcreteLongValue
import com.github.valentinaebi.capybara.values.ConcreteNullValue
import com.github.valentinaebi.capybara.values.ConcreteValue
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.RawValueType
import com.github.valentinaebi.capybara.values.RawValueType.Double
import com.github.valentinaebi.capybara.values.RawValueType.Float
import com.github.valentinaebi.capybara.values.RawValueType.Int32
import com.github.valentinaebi.capybara.values.RawValueType.Long
import com.github.valentinaebi.capybara.values.RawValueType.Reference
import com.github.valentinaebi.capybara.values.SecondBytePlaceholder
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
import com.github.valentinaebi.capybara.values.valueForType
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

class SymbolicInterpreter(
    private val constraintsRepository: ConstraintsRepository,
    private val stringsRepository: StringsRepository,
    private val ownedObjectsRepository: OwnedObjectsRepository,
    private val arraysRepository: ArraysRepository
) : Interpreter<ProgramValue>(API_LEVEL) {

    val raisedException: ReferenceType? get() = _raisedException
    private var _raisedException: ReferenceType? = null


    override fun newValue(type: Type?): ProgramValue {
        return if (type == null) {
            SecondBytePlaceholder
        } else {
            AtomicSymbolicValue(RawValueType.fromAsmSort(type.sort))
        }
    }

    override fun newOperation(insn: AbstractInsnNode?): ProgramValue {
        val opcode = insn!!.opcode
        return when (opcode) {
            Opcodes.ACONST_NULL -> ConcreteNullValue
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
                        val strValue = AtomicSymbolicValue(Reference)
                        stringsRepository.addString(strValue, cst)
                        strValue
                    }

                    else -> AtomicSymbolicValue(Reference)
                }
            }

            Opcodes.GETSTATIC -> {
                val descriptor = (insn as FieldInsnNode).desc
                val sort = Type.getType(descriptor).sort
                return AtomicSymbolicValue(RawValueType.fromAsmSort(sort))
            }

            Opcodes.NEW -> AtomicSymbolicValue(Reference)
            else -> throw AssertionError("unexpected opcode: ${Printer.OPCODES[opcode]}")
        }
    }

    override fun copyOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue = value!!

    override fun unaryOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue {
        /*
        ARRAYLENGTH, CHECKCAST, INSTANCEOF, MONITORENTER, MONITOREXIT
         */
        requireNotNull(value)
        val opcode = insn!!.opcode
        return when (opcode) {
            in Opcodes.INEG..Opcodes.DNEG -> -value
            Opcodes.IINC -> value + one_int
            Opcodes.I2L -> mkLong(value, constraintsRepository)
            Opcodes.I2F, Opcodes.L2F -> convertIfConcrete(value, Float)
            Opcodes.I2D, Opcodes.L2D -> convertIfConcrete(value, Double)
            Opcodes.L2I -> mkInt32(value, constraintsRepository)
            Opcodes.F2I, Opcodes.D2I -> convertIfConcrete(value, Int32)
            Opcodes.F2L, Opcodes.D2L -> convertIfConcrete(value, Long)
            Opcodes.F2D -> mkDouble(value, constraintsRepository)
            Opcodes.D2F -> mkFloat(value, constraintsRepository)
            Opcodes.I2B -> truncateIfConcrete(value) { it.toByte() }
            Opcodes.I2C -> truncateIfConcrete(value) { it.toChar().code }
            Opcodes.I2S -> truncateIfConcrete(value) { it.toShort() }
            Opcodes.PUTSTATIC -> {
                if (value.rawValueType == Reference) {
                    ownedObjectsRepository.markAsLeaked(value)
                    arraysRepository.markAsLeaked(value)
                }
                placeholderValue
            }

            Opcodes.GETFIELD -> {
                val fieldInsnNode = insn as FieldInsnNode
                ownedObjectsRepository.getFieldValue(value, fieldInsnNode.name)
                    ?: AtomicSymbolicValue(RawValueType.fromDescriptor(fieldInsnNode.desc))
            }

            Opcodes.NEWARRAY, Opcodes.ANEWARRAY -> {
                // TODO save representation of known arrays
                val array = AtomicSymbolicValue(Reference)
                arraysRepository.saveArrayOfLen(array, value)
                array
            }

            Opcodes.ARRAYLENGTH -> arraysRepository.lengthOf(value) ?: AtomicSymbolicValue(Int32)
            else -> TODO()
        }
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
        require(value2!!.size == value3!!.size)
        TODO()
    }

    override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out ProgramValue>?): ProgramValue {
        TODO()
    }

    override fun returnOperation(insn: AbstractInsnNode?, value: ProgramValue?, expected: ProgramValue?) {
        throw UnsupportedOperationException("returnOperation is not supposed to be called in ${SymbolicInterpreter.javaClass.name}")
    }

    override fun merge(value1: ProgramValue?, value2: ProgramValue?): ProgramValue {
        throw UnsupportedOperationException("merged is not supposed to be called in ${SymbolicInterpreter.javaClass.name}")
    }

    private fun convertIfConcrete(v: ProgramValue, desiredType: RawValueType): ProgramValue {
        if (v is ConcreteValue) {
            return valueForType(desiredType, v.value)
        }
        return AtomicSymbolicValue(desiredType)
    }

    private fun truncateIfConcrete(v: ProgramValue, truncator: (Int) -> Number): ProgramValue {
        if (v is ConcreteInt32BitsValue) {
            return ConcreteInt32BitsValue(truncator(v.value).toInt())
        }
        return AtomicSymbolicValue(Int32)
    }

    private companion object {
        private val placeholderValue = AtomicSymbolicValue(RawValueType.Placeholder)
    }

}
