package com.github.valentinaebi.capybara.values

import com.github.valentinaebi.capybara.InternalName
import io.ksmt.KContext
import io.ksmt.expr.KInt32NumExpr
import io.ksmt.expr.KInt64NumExpr
import org.objectweb.asm.Type

class ValuesCreator(private val ctx: KContext) {

    private var nextSvIdx = 0

    private val arrayLenFunc = with(ctx) { ctx.mkFuncDecl("arrayLen", intSort, listOf(bv32Sort)) }

    fun mkIntValue(v: Int): Int32Value = with(ctx) { Int32Value(v.expr) }
    fun mkLongValue(v: Long): LongValue = with(ctx) { LongValue(v.expr) }
    fun mkFloatValue(v: Float): FloatValue = with(ctx) { FloatValue(v.expr) }
    fun mkDoubleValue(v: Double): DoubleValue = with(ctx) { DoubleValue(v.expr) }

    fun mkSymbolicInt32(): Int32Value = with(ctx) {
        val namePrefix = "sv#${nextSvIdx++}_"
        Int32Value(mkConst(namePrefix + "i32", intSort))
    }

    fun mkSymbolicLong(): LongValue = with(ctx) {
        val namePrefix = "sv#${nextSvIdx++}_"
        LongValue(mkConst(namePrefix + "i64", intSort))
    }

    fun mkSymbolicFloat(): FloatValue = with(ctx) {
        val namePrefix = "sv#${nextSvIdx++}_"
        FloatValue(mkConst(namePrefix + "f32", fp32Sort))
    }

    fun mkSymbolicDouble(): DoubleValue = with(ctx) {
        val namePrefix = "sv#${nextSvIdx++}_"
        DoubleValue(mkConst(namePrefix + "f64", fp64Sort))
    }

    fun mkSymbolicRef(): ReferenceValue = with(ctx) {
        val namePrefix = "sv#${nextSvIdx++}_"
        ReferenceValue(mkConst(namePrefix + "ref", bv32Sort))
    }

    fun mkSymbolicValue(asmSort: Int): ProgramValue = when (asmSort) {
        Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> mkSymbolicInt32()
        Type.LONG -> mkSymbolicLong()
        Type.FLOAT -> mkSymbolicFloat()
        Type.DOUBLE -> mkSymbolicDouble()
        Type.ARRAY, Type.OBJECT -> mkSymbolicRef()
        else -> throw IllegalArgumentException("unexpected asmSort: $asmSort")
    }

    fun mkSymbolicValue(desc: InternalName): ProgramValue = mkSymbolicValue(Type.getType(desc).sort)

    fun arrayLen(array: ProgramValue): ProgramValue {
        val lenOfArray = ctx.mkApp(arrayLenFunc, listOf(array.ksmtValue))
        return Int32Value(lenOfArray)
    }

    val minusOne_int = mkIntValue(-1)
    val zero_int = mkIntValue(0)
    val one_int = mkIntValue(1)
    val two_int = mkIntValue(2)
    val three_int = mkIntValue(3)
    val four_int = mkIntValue(4)
    val five_int = mkIntValue(5)
    val zero_long = mkLongValue(0)
    val one_long = mkLongValue(1)
    val zero_float = mkFloatValue(0f)
    val one_float = mkFloatValue(1f)
    val two_float = mkFloatValue(2f)
    val zero_double = mkDoubleValue(0.0)
    val one_double = mkDoubleValue(1.0)
    val nullValue = mkSymbolicRef()
    val placeholderValue = mkSymbolicRef()

    fun int2Float(v: Int32Value): FloatValue {
        val iVal = v.ksmtValue
        return when (iVal) {
            is KInt32NumExpr -> mkFloatValue(iVal.value.toFloat())
            else -> mkSymbolicFloat()
        }
    }

    fun long2Float(v: LongValue): DoubleValue {
        val iVal = v.ksmtValue
        return when (iVal) {
            is KInt64NumExpr -> mkDoubleValue(iVal.value.toDouble())
            else -> mkSymbolicDouble()
        }
    }

}
