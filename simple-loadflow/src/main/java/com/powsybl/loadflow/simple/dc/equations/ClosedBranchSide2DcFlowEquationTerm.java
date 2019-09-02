/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.simple.dc.equations;

import com.powsybl.loadflow.simple.equations.EquationContext;
import com.powsybl.loadflow.simple.equations.Variable;
import com.powsybl.loadflow.simple.network.LfBranch;
import com.powsybl.loadflow.simple.network.LfBus;

import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public final class ClosedBranchSide2DcFlowEquationTerm extends AbstractClosedBranchDcFlowEquationTerm {

    private double p2;

    private ClosedBranchSide2DcFlowEquationTerm(LfBranch branch, LfBus bus1, LfBus bus2, EquationContext equationContext) {
        super(branch, bus1, bus2, equationContext);
    }

    public static ClosedBranchSide2DcFlowEquationTerm create(LfBranch branch, LfBus bus1, LfBus bus2, EquationContext equationContext) {
        Objects.requireNonNull(branch);
        Objects.requireNonNull(bus1);
        Objects.requireNonNull(bus2);
        Objects.requireNonNull(equationContext);
        return new ClosedBranchSide2DcFlowEquationTerm(branch, bus1, bus2, equationContext);
    }

    @Override
    public void update(double[] x) {
        Objects.requireNonNull(x);
        double ph1 = x[ph1Var.getColumn()];
        double ph2 = x[ph2Var.getColumn()];
        double deltaPhase =  ph2 - ph1 + branch.a2() - branch.a1();
        p2 = power * deltaPhase;
    }

    @Override
    public double eval() {
        return p2;
    }

    @Override
    public double der(Variable variable) {
        Objects.requireNonNull(variable);
        if (variable.equals(ph1Var)) {
            return -power;
        } else if (variable.equals(ph2Var)) {
            return power;
        } else {
            throw new IllegalStateException("Unknown variable: " + variable);
        }
    }

    @Override
    public double rhs(Variable variable) {
        Objects.requireNonNull(variable);
        if (variable.equals(ph2Var)) {
            return power * (branch.a2() - branch.a1());
        }
        return 0;
    }
}
