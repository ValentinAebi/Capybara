package com.github.valentinaebi.capybara.values

import io.ksmt.expr.*
import io.ksmt.sort.*
import org.objectweb.asm.tree.analysis.Value

sealed interface ProgramValue : Value {
    val ksmtValue: KExpr<*>
    val shortCode: String

    fun int32(): Int32Value = this as Int32Value
    fun long(): LongValue = this as LongValue
    fun float(): FloatValue = this as FloatValue
    fun double(): DoubleValue = this as DoubleValue
    fun ref(): ReferenceValue = this as ReferenceValue
}

sealed interface NumericValue<S : KSort> : ProgramValue {
    override val ksmtValue: KExpr<S>
    val constVal: Number?
}

data class Int32Value(override val ksmtValue: KExpr<KIntSort>) : NumericValue<KIntSort> {
    override fun getSize(): Int = 1
    override val shortCode: String = "i32"
    override val constVal: Int?
        get() = when (ksmtValue) {
            is KInt32NumExpr -> ksmtValue.value
            else -> null
        }
}

data class LongValue(override val ksmtValue: KExpr<KIntSort>) : NumericValue<KIntSort> {
    override fun getSize(): Int = 2
    override val shortCode: String = "i64"
    override val constVal: Long?
        get() = when (ksmtValue) {
            is KInt64NumExpr -> ksmtValue.value
            else -> null
        }
}

data class FloatValue(override val ksmtValue: KExpr<KFp32Sort>) : NumericValue<KFp32Sort> {
    override fun getSize(): Int = 1
    override val shortCode: String = "f32"
    override val constVal: Number?
        get() = when (ksmtValue) {
            is KFp32Value -> ksmtValue.value
            else -> null
        }
}

data class DoubleValue(override val ksmtValue: KExpr<KFp64Sort>) : NumericValue<KFp64Sort> {
    override fun getSize(): Int = 2
    override val shortCode: String = "f64"
    override val constVal: Number?
        get() = when (ksmtValue) {
            is KFp64Value -> ksmtValue.value
            else -> null
        }
}

data class ReferenceValue(override val ksmtValue: KExpr<KBv32Sort>) : ProgramValue {
    override fun getSize(): Int = 1
    override val shortCode: String = "ref"
}
