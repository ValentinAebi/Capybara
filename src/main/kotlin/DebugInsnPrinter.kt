package com.github.valentinaebi.capybara

import org.objectweb.asm.util.Textifier


class DebugInsnPrinter : Textifier(API_LEVEL) {

    fun makeSeparator(){
        stringBuilder.setLength(0)
        stringBuilder.append(" >> new block\n")
        text.add(stringBuilder.toString());
    }

}
