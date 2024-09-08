package com.github.valentinaebi.capybara.loading

import com.github.valentinaebi.capybara.API_LEVEL
import com.github.valentinaebi.capybara.InternalName
import com.github.valentinaebi.capybara.SubtypingRelationBuilder
import com.github.valentinaebi.capybara.programstruct.Class
import com.github.valentinaebi.capybara.programstruct.Method
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodNode


class ClassBuilder(private val subtypingMap: SubtypingRelationBuilder) :
    ClassVisitor(API_LEVEL) {
    private var classInternalName: String? = null
    private val fields: MutableMap<String, InternalName> = linkedMapOf()
    private val methods: LinkedHashMap<String, Method> = linkedMapOf()
    private var methodsMayBeOverriden: Boolean = true

    fun toClass(): Class = Class(classInternalName!!, fields, methods)

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
        methods[name!!] = Method(methodNode, mayBeOverridden)
        return MethodBuilder(methodNode)
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        fields[name!!] = Type.getType(descriptor).internalName
        return super.visitField(access, name, descriptor, signature, value)
    }

    private fun saveSubtyping(subT: InternalName, superT: InternalName) {
        subtypingMap.getOrPut(subT) { mutableSetOf() }.add(superT)
    }

}
