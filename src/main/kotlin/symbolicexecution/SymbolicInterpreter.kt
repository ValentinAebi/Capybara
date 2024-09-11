package com.github.valentinaebi.capybara.symbolicexecution

import com.github.valentinaebi.capybara.API_LEVEL
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
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.analysis.Interpreter
import org.objectweb.asm.util.Printer

class SymbolicInterpreter(
    private val reporter: Reporter,
    private val valuesCreator: ValuesCreator,
    private val operatorsContext: OperatorsContext,
    private val checker: Checker,
    private val solver: Solver
) : Interpreter<ProgramValue>(API_LEVEL) {

    var lineResolver: ((AbstractInsnNode) -> Int)? = null

    override fun newValue(type: Type?): ProgramValue {
        return if (type == null) {
            valuesCreator.placeholderValue
        } else {
            valuesCreator.mkSymbolicValue(type.sort)
        }
    }

    override fun newOperation(insn: AbstractInsnNode?): ProgramValue {
        updateLine(insn!!)
        val opcode = insn.opcode
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

    override fun copyOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue {
        updateLine(insn!!)
        return value!!
    }

    override fun unaryOperation(insn: AbstractInsnNode?, value: ProgramValue?): ProgramValue {
        updateLine(insn!!)
        requireNotNull(value)
        return with(valuesCreator) {
            with(operatorsContext) {
                val opcode = insn.opcode
                when (opcode) {
                    Opcodes.INEG -> -value.int32()
                    Opcodes.LNEG -> -value.long()
                    Opcodes.FNEG -> -value.float()
                    Opcodes.DNEG -> -value.double()
                    Opcodes.IINC -> value.int32() + one_int
                    // TODO more conversion functions
                    Opcodes.I2L -> LongValue(value.int32().ksmtValue)
                    Opcodes.I2F -> int2Float(value.int32())
                    Opcodes.L2F -> long2Float(value.long())
                    Opcodes.I2D -> mkSymbolicDouble()
                    Opcodes.L2D -> mkSymbolicDouble()
                    Opcodes.L2I -> Int32Value(value.long().ksmtValue)   // FIXME technically incorrect
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
                        checker.mustBeNonNull(value.ref(), Check.FLD_NULL_OWNER)
                        val fieldInsnNode = insn as FieldInsnNode
                        // TODO check if object is owned
                        mkSymbolicValue(fieldInsnNode.desc)
                    }

                    Opcodes.NEWARRAY, Opcodes.ANEWARRAY -> {
                        val array = mkSymbolicRef()
                        solver.saveArrayLength(array, value.int32())
                        // TODO check that length >= 0
                        // TODO add to owned objects
                        array
                    }

                    Opcodes.ARRAYLENGTH -> valuesCreator.arrayLen(value.ref())
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
        updateLine(insn!!)
        requireNotNull(l)
        requireNotNull(r)
        assert(l.size == r.size)
        return with(valuesCreator) {
            with(operatorsContext) {
                val opcode = insn.opcode
                when (opcode) {
                    // TODO load value if array is owned
                    Opcodes.IALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD -> {
                        checker.arrayIndexingPrecondition(l, r)
                        mkSymbolicInt32()
                    }

                    Opcodes.LALOAD -> {
                        checker.arrayIndexingPrecondition(l, r)
                        mkSymbolicLong()
                    }

                    Opcodes.FALOAD -> {
                        checker.arrayIndexingPrecondition(l, r)
                        mkSymbolicFloat()
                    }

                    Opcodes.DALOAD -> {
                        checker.arrayIndexingPrecondition(l, r)
                        mkSymbolicDouble()
                    }

                    Opcodes.AALOAD -> {
                        checker.arrayIndexingPrecondition(l, r)
                        mkSymbolicRef()
                    }

                    Opcodes.IADD -> l.int32() + r.int32()
                    Opcodes.LADD -> l.long() + r.long()
                    Opcodes.FADD -> l.float() + r.float()
                    Opcodes.DADD -> l.double() + r.double()
                    Opcodes.ISUB -> l.int32() - r.int32()
                    Opcodes.LSUB -> l.long() - r.long()
                    Opcodes.FSUB -> l.float() - r.float()
                    Opcodes.DSUB -> l.double() - r.double()
                    Opcodes.IMUL -> l.int32() * r.int32()
                    Opcodes.LMUL -> l.long() * r.long()
                    Opcodes.FMUL -> l.float() * r.float()
                    Opcodes.DMUL -> l.double() * r.double()
                    Opcodes.IDIV -> l.int32() / r.int32()
                    Opcodes.LDIV -> l.long() / r.long()
                    Opcodes.FDIV -> l.float() / r.float()
                    Opcodes.DDIV -> l.double() / r.double()
                    Opcodes.IREM -> l.int32() % r.int32()
                    Opcodes.LREM -> l.long() % r.long()
                    Opcodes.FREM -> l.float() % r.float()
                    Opcodes.DREM -> l.double() % r.double()
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
                        checker.mustBeNonNull(l.ref(), Check.FLD_NULL_OWNER)
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
        updateLine(insn!!)
        requireNotNull(value1)
        requireNotNull(value2)
        requireNotNull(value3)
        /* IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE */
        checker.arrayIndexingPrecondition(value1, value2)
        // TODO save if array is owned
        return valuesCreator.placeholderValue
    }

    override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out ProgramValue>?): ProgramValue {
        updateLine(insn!!)
        requireNotNull(values)
        return with(valuesCreator) {
            with(operatorsContext) {
                // TODO consider inferred method contracts
                val opcode = insn.opcode
                when (opcode) {
                    in Opcodes.INVOKEVIRTUAL..Opcodes.INVOKEINTERFACE -> {
                        if (opcode != Opcodes.INVOKESTATIC) {
                            checker.mustBeNonNull(values.first().ref(), Check.INVK_NULL_REC)
                        }
                        val methodDesc = (insn as MethodInsnNode).desc
                        val retType = Type.getReturnType(methodDesc)
                        if (retType == Type.VOID_TYPE) placeholderValue else mkSymbolicValue(retType.sort)
                    }

                    Opcodes.INVOKEDYNAMIC -> {
                        val methodDesc = (insn as InvokeDynamicInsnNode).desc
                        val retType = Type.getReturnType(methodDesc)
                        if (retType == Type.VOID_TYPE) placeholderValue else mkSymbolicValue(retType.sort)
                    }

                    Opcodes.MULTIANEWARRAY -> {
                        // TODO save length
                        mkSymbolicRef()
                    }

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

    private fun updateLine(insn: AbstractInsnNode) {
        reporter.currentLine = lineResolver!!(insn)
    }

}
