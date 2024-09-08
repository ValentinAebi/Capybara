package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.API_LEVEL
import com.github.valentinaebi.capybara.InternalName
import com.github.valentinaebi.capybara.checks.Reporter
import com.github.valentinaebi.capybara.solving.Solver
import com.github.valentinaebi.capybara.values.Int32Value
import com.github.valentinaebi.capybara.values.LongValue
import com.github.valentinaebi.capybara.values.ProgramValue
import com.github.valentinaebi.capybara.values.ValuesCreator
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.util.Printer

class SymbolicInterpreter(
    private val reporter: Reporter,
    private val solver: Solver,
    private val valuesCreator: ValuesCreator,
    private val operatorsContext: OperatorsContext
) : Interpreter<ProgramValue>(API_LEVEL) {

    val raisedException: InternalName? get() = _raisedException
    private var _raisedException: InternalName? = null

    override fun newValue(type: Type?): ProgramValue {
        return if (type == null) {
            valuesCreator.placeholderValue
        } else {
            valuesCreator.mkSymbolicValue(type.sort)
        }
    }

    override fun newOperation(insn: AbstractInsnNode?): ProgramValue {
        val opcode = insn!!.opcode
        return with(valuesCreator) {
            when (opcode) {
                Opcodes.ACONST_NULL -> nullValue
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
                Opcodes.BIPUSH -> mkIntValue((insn as IntInsnNode).operand)
                Opcodes.SIPUSH -> mkIntValue((insn as IntInsnNode).operand)
                Opcodes.LDC -> {
                    val cst = (insn as LdcInsnNode).cst
                    when (cst) {
                        is Int -> mkIntValue(cst)
                        is Long -> mkLongValue(cst)
                        is Float -> mkFloatValue(cst)
                        is Double -> mkDoubleValue(cst)
                        is String -> {
                            val strValue = mkSymbolicRef()
                            // TODO save string value
                            // TODO add type constraint (String)
                            strValue
                        }

                        else -> mkSymbolicRef()
                    }
                }

                Opcodes.GETSTATIC -> {
                    val descriptor = (insn as FieldInsnNode).desc
                    return mkSymbolicValue(descriptor)
                }

                Opcodes.NEW -> {
                    val typeDescriptor = (insn as TypeInsnNode).desc
                    val newObj = mkSymbolicRef()
                    // TODO save type
                    newObj
                }

                else -> throw AssertionError("unexpected opcode: ${Printer.OPCODES[opcode]}")
            }
        }
    }

    override fun copyOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue = value!!

    override fun unaryOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue {
        requireNotNull(value)
        return with(valuesCreator) {
            with(operatorsContext) {
                val opcode = insn!!.opcode
                when (opcode) {
                    in Opcodes.INEG..Opcodes.DNEG -> -value
                    Opcodes.IINC -> value + one_int
                    // TODO more conversion functions
                    Opcodes.I2L -> LongValue((value as Int32Value).ksmtValue)
                    Opcodes.I2F -> int2Float(value as Int32Value)
                    Opcodes.L2F -> long2Float(value as LongValue)
                    Opcodes.I2D -> mkSymbolicDouble()
                    Opcodes.L2D -> mkSymbolicDouble()
                    Opcodes.L2I -> Int32Value((value as LongValue).ksmtValue)   // FIXME technically incorrect
                    Opcodes.F2I -> mkSymbolicInt32()
                    Opcodes.D2I -> mkSymbolicInt32()
                    Opcodes.F2L -> mkSymbolicLong()
                    Opcodes.D2L -> mkSymbolicLong()
                    Opcodes.F2D -> mkSymbolicDouble()
                    Opcodes.D2F -> mkSymbolicFloat()
                    Opcodes.I2B -> mkSymbolicInt32()
                    Opcodes.I2C -> mkSymbolicInt32()
                    Opcodes.I2S -> mkSymbolicInt32()
                    Opcodes.PUTSTATIC -> {
                        // TODO save leak
                        placeholderValue
                    }

                    Opcodes.GETFIELD -> {
                        // TODO check that value != null
                        val fieldInsnNode = insn as FieldInsnNode
                        // TODO check if object is owned
                        mkSymbolicValue(fieldInsnNode.desc)
                    }

                    Opcodes.NEWARRAY, Opcodes.ANEWARRAY -> {
                        // TODO check that length >= 0
                        // TODO add to owned objects
                        mkSymbolicRef()
                    }

                    Opcodes.ARRAYLENGTH -> valuesCreator.arrayLen(value)
                    Opcodes.CHECKCAST -> {
                        // TODO check cast is possible
                        // TODO save new type
                        value
                    }

                    Opcodes.INSTANCEOF -> {
                        // TODO check that type is possible
                        // TODO propagate type constraint
                        mkSymbolicInt32()
                    }

                    Opcodes.MONITORENTER, Opcodes.MONITOREXIT -> placeholderValue

                    else -> throw AssertionError("unexpected opcode: ${Printer.OPCODES[opcode]}")
                }
            }
        }
    }

    override fun binaryOperation(insn: AbstractInsnNode?, l: ProgramValue?, r: ProgramValue?): ProgramValue {
        requireNotNull(l)
        requireNotNull(r)
        assert(l.size == r.size)
        return with(valuesCreator) {
            with(operatorsContext) {
                val opcode = insn!!.opcode
                when (opcode) {
                    // TODO check that array is not null
                    // TODO check that index is in bounds
                    // TODO load value if array is owned
                    Opcodes.IALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD -> mkSymbolicInt32()
                    Opcodes.LALOAD -> mkSymbolicLong()
                    Opcodes.FALOAD -> mkSymbolicFloat()
                    Opcodes.DALOAD -> mkSymbolicDouble()
                    Opcodes.AALOAD -> mkSymbolicRef()
                    in Opcodes.IADD..Opcodes.DADD -> l + r
                    in Opcodes.ISUB..Opcodes.DSUB -> l - r
                    in Opcodes.IMUL..Opcodes.DMUL -> l * r
                    in Opcodes.IDIV..Opcodes.DDIV -> l / r
                    in Opcodes.IREM..Opcodes.DREM -> l % r
                    // TODO also interpret these operations
                    Opcodes.ISHL -> mkSymbolicInt32()
                    Opcodes.LSHL -> mkSymbolicLong()
                    Opcodes.ISHR -> mkSymbolicInt32()
                    Opcodes.LSHR -> mkSymbolicLong()
                    Opcodes.IUSHR -> mkSymbolicInt32()
                    Opcodes.LUSHR -> mkSymbolicLong()
                    Opcodes.IAND -> mkSymbolicInt32()
                    Opcodes.LAND -> mkSymbolicLong()
                    Opcodes.IOR -> mkSymbolicInt32()
                    Opcodes.LOR -> mkSymbolicLong()
                    Opcodes.IXOR -> mkSymbolicInt32()
                    Opcodes.LXOR -> mkSymbolicLong()
                    /* LCMP, FCMPL, FCMPG, DCMPL, DCMPG, PUTFIELD */
                    // TODO handle these using conditional values
                    Opcodes.LCMP -> mkSymbolicInt32()
                    Opcodes.FCMPL -> mkSymbolicInt32()
                    Opcodes.FCMPG -> mkSymbolicInt32()
                    Opcodes.DCMPL -> mkSymbolicInt32()
                    Opcodes.DCMPG -> mkSymbolicInt32()
                    Opcodes.PUTFIELD -> {
                        // TODO check that receiver != null
                        // TODO mark as leaked
                        placeholderValue
                    }

                    else -> throw AssertionError("unexpected opcode: ${Printer.OPCODES[opcode]}")
                }
            }
        }
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode?,
        value1: ProgramValue?,
        value2: ProgramValue?,
        value3: ProgramValue?
    ): ProgramValue {
        requireNotNull(value1)
        requireNotNull(value2)
        requireNotNull(value3)
        /* IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE */
        // TODO check that array != null
        // TODO check that index is in bounds
        // TODO save if array is owned
        return valuesCreator.placeholderValue
    }

    override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out ProgramValue>?): ProgramValue {
        requireNotNull(values)
        return with(valuesCreator) {
            with(operatorsContext) {
                // TODO consider inferred method contracts
                val opcode = insn!!.opcode
                when (opcode) {
                    in Opcodes.INVOKEVIRTUAL..Opcodes.INVOKEINTERFACE -> {
                        val methodDesc = (insn as TypeInsnNode).desc
                        val retType = Type.getReturnType(methodDesc)
                        if (retType == Type.VOID_TYPE) placeholderValue else mkSymbolicValue(retType.sort)
                    }

                    Opcodes.INVOKEDYNAMIC -> {
                        val methodDesc = (insn as InvokeDynamicInsnNode).desc
                        val retType = Type.getReturnType(methodDesc)
                        if (retType == Type.VOID_TYPE) placeholderValue else mkSymbolicValue(retType.sort)
                    }

                    Opcodes.MULTIANEWARRAY -> mkSymbolicRef()
                    else -> throw AssertionError("unexpected opcode: ${Printer.OPCODES[opcode]}")
                }
            }
        }
    }

    override fun returnOperation(insn: AbstractInsnNode?, value: ProgramValue?, expected: ProgramValue?) {
        throw UnsupportedOperationException("returnOperation is not supposed to be called in ${SymbolicInterpreter::class.simpleName}")
    }

    override fun merge(value1: ProgramValue?, value2: ProgramValue?): ProgramValue {
        throw UnsupportedOperationException("merged is not supposed to be called in ${SymbolicInterpreter::class.simpleName}")
    }

}
