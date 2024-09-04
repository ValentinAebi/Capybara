package com.github.valentinaebi.capybara.cfg

import com.github.valentinaebi.capybara.API_LEVEL
import com.github.valentinaebi.capybara.execution.SymbolicInterpreter
import com.github.valentinaebi.capybara.values.ProgramValue
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.Frame
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter

class BasicBlock(
    val insnList: LinkedHashMap<AbstractInsnNode, Int>,
    val terminator: BasicBlockTerminator,
    val surroundingCatch: Catch?,
    val basicBlockIdx: Int
) {

    override fun toString(): String = "Block#$basicBlockIdx"

    fun fullDescr(): String {
        val printer = BasicBlockTextifier()
        val visitor = TraceMethodVisitor(printer)
        var currLine = -1
        for ((insn, lineIdx) in insnList) {
            if (lineIdx != currLine) {
                printer.markNewLine(lineIdx)
                currLine = lineIdx
            }
            insn.accept(visitor)
        }
        val stringWriter = StringWriter()
        stringWriter.append("Block#").append(basicBlockIdx.toString())
        if (surroundingCatch != null) {
            stringWriter.append(" [surrounded by ").append(surroundingCatch.toString()).append("]")
        }
        stringWriter.append(":\n")
        printer.print(PrintWriter(stringWriter))
        stringWriter.append(" end: ").append(terminator.fullDescr().prependIndent("  ")).append("\n")
        return stringWriter.toString()
    }

    fun simulateInstructions(
        frame: Frame<ProgramValue>,
        interpreter: SymbolicInterpreter
    ) {
        for ((insn, _) in insnList) {
            frame.execute(insn, interpreter)
            if (interpreter.raisedException != null) {
                break
            }
        }
    }

    companion object {

        private class BasicBlockTextifier : Textifier(API_LEVEL) {

            fun markNewLine(line: Int) {
                stringBuilder.setLength(0)
                stringBuilder.append("  (line ").append(line).append(")\n")
                text.add(stringBuilder.toString())
            }

        }

    }

}
