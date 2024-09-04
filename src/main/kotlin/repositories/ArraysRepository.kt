package com.github.valentinaebi.capybara.repositories

import com.github.valentinaebi.capybara.TypeDescriptor
import com.github.valentinaebi.capybara.values.ProgramValue

class ArraysRepository {
    private val arrays: MutableMap<ProgramValue, ArrayRepresentation> = mutableMapOf()

    fun saveOwnedArrayOfLenAndElemType(array: ProgramValue, len: ProgramValue, typeDescriptor: TypeDescriptor?) {
        val repres = ArrayRepresentation(typeDescriptor, isInitiallyOwned = true)
        repres.saveLength(len)
        arrays[array] = repres
    }

    fun lengthOf(array: ProgramValue): ProgramValue? = arrays[array]?.length

    fun markAsLeaked(array: ProgramValue) {
        arrays[array]?.markAsLeaked()
    }

    fun saveLength(array: ProgramValue, len: ProgramValue) {
        arrays[array]?.saveLength(len)
    }

    fun saveCellValueIfOwned(array: ProgramValue, idx: ProgramValue, value: ProgramValue) {
        arrays[array]?.saveCellValueIfOwned(idx, value)
    }

    fun getCellValueIfOwned(array: ProgramValue, idx: ProgramValue): ProgramValue? =
        arrays[array]?.getCellValueIfKnown(idx)

    private class ArrayRepresentation(val elemType: TypeDescriptor?, isInitiallyOwned: Boolean) {
        private var _length: ProgramValue? = null
        private var idxToknownValues: MutableMap<ProgramValue, ProgramValue>? =
            if (isInitiallyOwned) mutableMapOf() else null

        val length: ProgramValue? get() = _length

        fun markAsLeaked() {
            idxToknownValues = null
        }

        fun saveLength(len: ProgramValue) {
            if (_length == null) {
                _length = len
            } else {
                assert(len == _length)
            }
        }

        fun saveCellValueIfOwned(idx: ProgramValue, value: ProgramValue) {
            val idxToknownValues = idxToknownValues
            if (idxToknownValues != null) {
                idxToknownValues[idx] = value
            }
        }

        fun getCellValueIfKnown(idx: ProgramValue): ProgramValue? = idxToknownValues?.get(idx)

    }

}
