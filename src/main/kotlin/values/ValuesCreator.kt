package com.github.valentinaebi.capybara.values

import com.github.valentinaebi.capybara.InternalName
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KInt32NumExpr
import io.ksmt.expr.KInt64NumExpr
import io.ksmt.sort.KBoolSort
import org.objectweb.asm.Type

class ValuesCreator(private val ctx: KContext) {

    private var nextSvIdx = 0

    private val arrayLenFunc = with(ctx) { ctx.mkFuncDecl("arrayLen", intSort, listOf(bv32Sort)) }

    fun mkIntValue(v: Int): Int32Value = with(ctx) { Int32Value(v.expr) }
    fun mkLongValue(v: Long): LongValue = with(ctx) { LongValue(v.expr) }
    fun mkFloatValue(v: Float): FloatValue = with(ctx) { FloatValue(v.expr) }
    fun mkDoubleValue(v: Double): DoubleValue = with(ctx) { DoubleValue(v.expr) }

    fun mkSymbolicInt32(idAnnot: String? = null): Int32Value = with(ctx) {
        Int32Value(mkConst(mkPrefix(idAnnot) + "i32", intSort))
    }

    fun mkSymbolicLong(idAnnot: String? = null): LongValue = with(ctx) {
        LongValue(mkConst(mkPrefix(idAnnot) + "i64", intSort))
    }

    fun mkSymbolicFloat(idAnnot: String? = null): FloatValue = with(ctx) {
        FloatValue(mkConst(mkPrefix(idAnnot) + "f32", fp32Sort))
    }

    fun mkSymbolicDouble(idAnnot: String? = null): DoubleValue = with(ctx) {
        DoubleValue(mkConst(mkPrefix(idAnnot) + "f64", fp64Sort))
    }

    fun mkSymbolicRef(idAnnot: String? = null): ReferenceValue = with(ctx) {
        ReferenceValue(mkConst(mkPrefix(idAnnot) + "ref", bv32Sort))
    }

    fun mkSymbolicValue(asmSort: Int, idAnnot: String? = null): ProgramValue = when (asmSort) {
        Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> mkSymbolicInt32(idAnnot)
        Type.LONG -> mkSymbolicLong(idAnnot)
        Type.FLOAT -> mkSymbolicFloat(idAnnot)
        Type.DOUBLE -> mkSymbolicDouble(idAnnot)
        Type.ARRAY, Type.OBJECT -> mkSymbolicRef(idAnnot)
        else -> throw IllegalArgumentException("unexpected asmSort: $asmSort")
    }

    fun mkSymbolicValue(desc: InternalName, idAnnot: String? = null): ProgramValue =
        mkSymbolicValue(Type.getType(desc).sort, idAnnot)

    private fun mkPrefix(idAnnot: String?): String =
        "sv#${nextSvIdx++}_" + (if (idAnnot == null) "" else "${idAnnot}_")

    fun arrayLen(array: ReferenceValue): Int32Value {
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
    val nullValue = mkSymbolicRef("null")
    val placeholderValue = mkSymbolicRef("placeholder")

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

    infix fun Int32Value.eq(r: Int32Value): KExpr<KBoolSort> {
        val l = this
        return with(ctx) { l.ksmtValue eq r.ksmtValue }
    }

    infix fun LongValue.eq(r: LongValue): KExpr<KBoolSort> {
        val l = this
        return with(ctx) { l.ksmtValue eq r.ksmtValue }
    }

    infix fun FloatValue.eq(r: FloatValue): KExpr<KBoolSort> {
        val l = this
        return with(ctx) { l.ksmtValue eq r.ksmtValue }
    }

    infix fun DoubleValue.eq(r: DoubleValue): KExpr<KBoolSort> {
        val l = this
        return with(ctx) { l.ksmtValue eq r.ksmtValue }
    }

    infix fun ReferenceValue.eq(r: ReferenceValue): KExpr<KBoolSort> {
        val l = this
        return with(ctx) { l.ksmtValue eq r.ksmtValue }
    }

    fun isZeroFormula(v: NumericValue<*>): KExpr<KBoolSort> {
        return when (v) {
            is Int32Value -> v eq zero_int
            is LongValue -> v eq zero_long
            is FloatValue -> v eq zero_float
            is DoubleValue -> v eq zero_double
        }
    }

    fun isLessThanZeroFormula(v: NumericValue<*>): KExpr<KBoolSort> {
        return when (v) {
            is Int32Value -> ctx.mkArithLt(v.ksmtValue, zero_int.ksmtValue)
            is LongValue -> ctx.mkArithLt(v.ksmtValue, zero_long.ksmtValue)
            is FloatValue -> ctx.mkFpLessExpr(v.ksmtValue, zero_float.ksmtValue)
            is DoubleValue -> ctx.mkFpLessExpr(v.ksmtValue, zero_double.ksmtValue)
        }
    }

    fun isGreaterThanZeroFormula(v: NumericValue<*>): KExpr<KBoolSort> {
        return when (v) {
            is Int32Value -> ctx.mkArithGt(v.ksmtValue, zero_int.ksmtValue)
            is LongValue -> ctx.mkArithGt(v.ksmtValue, zero_long.ksmtValue)
            is FloatValue -> ctx.mkFpGreaterExpr(v.ksmtValue, zero_float.ksmtValue)
            is DoubleValue -> ctx.mkFpGreaterExpr(v.ksmtValue, zero_double.ksmtValue)
        }
    }

    fun lessThanFormula(l: NumericValue<*>, r: NumericValue<*>): KExpr<KBoolSort> {
        return when {
            l is Int32Value && r is Int32Value -> ctx.mkArithLt(l.ksmtValue, r.ksmtValue)
            l is LongValue && r is LongValue -> ctx.mkArithLt(l.ksmtValue, r.ksmtValue)
            l is FloatValue && r is FloatValue -> ctx.mkFpLessExpr(l.ksmtValue, r.ksmtValue)
            l is DoubleValue && r is DoubleValue -> ctx.mkFpLessExpr(l.ksmtValue, r.ksmtValue)
            else -> throw AssertionError("wrong operand types: ${l::class.simpleName} < ${r::class.simpleName}")
        }
    }

    fun lessThanOrEqualToFormula(l: NumericValue<*>, r: NumericValue<*>): KExpr<KBoolSort> {
        return when {
            l is Int32Value && r is Int32Value -> ctx.mkArithLe(l.ksmtValue, r.ksmtValue)
            l is LongValue && r is LongValue -> ctx.mkArithLe(l.ksmtValue, r.ksmtValue)
            l is FloatValue && r is FloatValue -> ctx.mkFpLessOrEqualExpr(l.ksmtValue, r.ksmtValue)
            l is DoubleValue && r is DoubleValue -> ctx.mkFpLessOrEqualExpr(l.ksmtValue, r.ksmtValue)
            else -> throw AssertionError("wrong operand types: ${l::class.simpleName} < ${r::class.simpleName}")
        }
    }

    fun areEqualFormula(l: ProgramValue, r: ProgramValue): KExpr<KBoolSort> {
        return when {
            l is Int32Value && r is Int32Value -> l eq r
            l is LongValue && r is LongValue -> l eq r
            l is FloatValue && r is FloatValue -> l eq r
            l is DoubleValue && r is DoubleValue -> l eq r
            l is ReferenceValue && r is ReferenceValue -> l eq r
            else -> throw AssertionError("wrong operand types: ${l::class.simpleName} == ${r::class.simpleName}")
        }
    }

}
