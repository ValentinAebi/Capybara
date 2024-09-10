package com.github.valentinaebi.capybara.loading

import com.github.valentinaebi.capybara.API_LEVEL
import com.github.valentinaebi.capybara.InternalName
import com.github.valentinaebi.capybara.programstruct.Class
import com.github.valentinaebi.capybara.programstruct.Method
import com.github.valentinaebi.capybara.solving.SubtypingRelationBuilder
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode


class ClassBuilder(private val subtypingMap: SubtypingRelationBuilder) :
    ClassVisitor(API_LEVEL) {
    private var classInternalName: InternalName? = null
    private var srcFileName: String? = null
    private val fields: MutableMap<String, InternalName> = linkedMapOf()
    private val methods: LinkedHashMap<String, Method> = linkedMapOf()
    private var methodsMayBeOverriden: Boolean = true

    fun toClass(): Class = Class(classInternalName!!, fields, methods, srcFileName!!)

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String?>?
    ) {
        classInternalName = name!!
        if ((access and Opcodes.ACC_FINAL) != 0) {
            methodsMayBeOverriden = false
        }
        superName?.let { saveSubtyping(name, superName) }
        if (interfaces != null) {
            for (itf in interfaces) {
                itf?.let { saveSubtyping(name, itf) }
            }
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methodNode = MethodNode(access, name, descriptor, signature, exceptions)
        val mayBeOverridden =
            methodsMayBeOverriden && (access and (Opcodes.ACC_FINAL or Opcodes.ACC_STATIC or Opcodes.ACC_PRIVATE) == 0)
        val hasReceiver = (access and Opcodes.ACC_STATIC) == 0
        return MethodBuilder(methodNode, mayBeOverridden, hasReceiver, methods)
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        fields[name] = Type.getType(descriptor).internalName
        return super.visitField(access, name, descriptor, signature, value)
    }

    override fun visitSource(source: String?, debug: String?) {
        srcFileName = source
    }

    private fun saveSubtyping(subT: InternalName, superT: InternalName) {
        subtypingMap.getOrPut(subT) { mutableSetOf() }.add(superT)
    }

}
