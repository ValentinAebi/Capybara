package com.github.valentinaebi.capybara.solving

import io.ksmt.KContext
import io.ksmt.solver.cvc5.KCvc5Solver

class Solver(private val ctx: KContext) {
    private val cvc5 = KCvc5Solver(ctx)

}
