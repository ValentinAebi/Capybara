package com.github.valentinaebi.capybara.loading

import com.github.valentinaebi.capybara.API_LEVEL
import com.github.valentinaebi.capybara.GraphBuilder
import com.github.valentinaebi.capybara.InternalName
import com.github.valentinaebi.capybara.programstruct.Class
import com.github.valentinaebi.capybara.programstruct.Method
import com.github.valentinaebi.capybara.programstruct.MethodIdentifier
import org.objectweb.asm.*
import org.objectweb.asm.tree.MethodNode


class ClassBuilder(
    private val subtypingRelation: GraphBuilder<InternalName>,
    private val callGraph: GraphBuilder<MethodIdentifier>
) : ClassVisitor(API_LEVEL) {
    private var classInternalName: InternalName? = null
    private var srcFileName: String? = null
    private val fields: MutableMap<String, InternalName> = linkedMapOf()
    private val methods: LinkedHashMap<MethodIdentifier, Method> = linkedMapOf()
    private var methodsMayBeOverriden: Boolean = true

    fun toClass(): Class = Class(classInternalName!!, fields, methods, srcFileName!!)

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String?>?
    ) {
        classInternalName = name
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
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methodNode = MethodNode(access, name, descriptor, signature, exceptions)
        val mayBeOverridden =
            methodsMayBeOverriden && (access and (Opcodes.ACC_FINAL or Opcodes.ACC_STATIC or Opcodes.ACC_PRIVATE) == 0)
        val hasReceiver = (access and Opcodes.ACC_STATIC) == 0
        val methodId = MethodIdentifier(classInternalName!!, name, descriptor)
        callGraph.addIsolatedVertex(methodId)
        return MethodBuilder(methodId, methodNode, mayBeOverridden, hasReceiver, methods, callGraph)
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
        subtypingRelation.addEdge(subT, superT)
    }

}
