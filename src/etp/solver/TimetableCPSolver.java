/*
 * Copyright (c) 2011 Sophie Demassey, Julien Menana, Mines de Nantes.
 *
 * This file is part of ChocoETP.
 *
 * ChocoETP is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * ChocoETP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ChocoETP.  If not, see <http://www.gnu.org/licenses/>.
 */

package etp.solver;

import choco.automaton.MultiCostRegular;
import choco.cp.solver.CPSolver;
import choco.cp.solver.search.integer.branching.AssignVar;
import choco.kernel.model.Model;
import choco.kernel.solver.branch.AbstractIntBranchingStrategy;
import choco.kernel.solver.branch.VarSelector;
import choco.kernel.solver.search.ValSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.kernel.solver.variables.integer.IntVar;
import etp.model.TimetableModel;
import etp.output.ETPOutput;
import etp.solver.search.MCRBasedRowValueSelector;

/** @author Sophie Demassey */
public class TimetableCPSolver extends CPSolver implements EtpSolver {

int nE;
IntDomainVar[][] shiftVars;
IntVar[][] counterVars;
IntVar[] objVars;
protected IntVar objVar;

int[][] shiftVals;
final ETPOutput writer;


public TimetableCPSolver(ETPOutput writer)
{
	super();
	this.writer = writer;
}

public IntDomainVar[][] getShiftVars()
{
	return shiftVars;
}

@Override
public Boolean solve()
{
	if (Boolean.TRUE != super.solve()) {
		LOGGER.warning("no solution found");
	} else {
		this.printRuntimeStatistics();
		this.solutionToFile();
	}
	return isFeasible();
}

public void read(Model model)
{
	super.read(model);
	TimetableModel m = (TimetableModel) model;
	nE = m.getShiftVars().length;
	shiftVars = new IntDomainVar[nE][];
	counterVars = new IntVar[nE][];
	objVars = new IntVar[nE];
	for (int e = 0; e < nE; e++) {
		shiftVars[e] = this.getVar(m.getShiftVars()[e]);
		objVars[e] = (m.getObjVars()[e] != null) ? this.getVar(m.getObjVars()[e]) : null;
		counterVars[e] = (m.getCounterVars()[e] != null) ? this.getVar(m.getCounterVars()[e]) : null;
	}
	objVar = (m.getObjVar() != null) ? this.getVar(m.getObjVar()) : null;
	shiftVals = new int[nE][shiftVars[0].length];
}

@Override
public String solutionToString()
{
	StringBuffer s = new StringBuffer(100);
	if (objVar != null && objVar.isInstantiated()) s.append("cost=").append(objVar.getVal());
	s.append("\n");
	for (int e = 0; e < nE; e++) {
		s.append(this.solutionToString(e)).append("\n");
	}
	return s.toString();
}

/**
 * print the current solver solution.
 * @return the string to print
 */
public String solutionToString(int e)
{
	super.solutionToString();
	StringBuffer s = new StringBuffer();
	for (IntVar v : shiftVars[e]) {
		s.append(v.getVal()).append(", ");
	}
	if (counterVars[e] != null) {
		s.append(" [");
		for (IntVar v : counterVars[e]) {
			s.append(v.getVal()).append(", ");
		}
		s.append("]");
	}
	if (objVars[e] != null) {
		s.append(" = ").append(objVars[e].getVal());
	}
	return s.toString();
}

/** print the current solver solution. */
public void storeSolution()
{
	for (int e = 0; e < shiftVars.length; e++) {
		for (int t = 0; t < shiftVars[e].length; t++) {
			shiftVals[e][t] = shiftVars[e][t].getVal();
		}
	}
}

public void solutionToFile()
{
	if (writer == null) return;
	this.storeSolution();
	writer.writeSolution(objVar.getVal(), this.shiftVals);
}

public AbstractIntBranchingStrategy getBranchingStrategy(String option)
{
	if (option.startsWith("MCR")) {
		String scoreType = (option.startsWith("MCR+")) ? option.substring(4) : "";
		return this.buildMcrBasedStrategy(scoreType);
	}
	return null;
}

protected AbstractIntBranchingStrategy buildMcrBasedStrategy(String scoreType)
{
	TimetableModel m = (TimetableModel) model;
	MultiCostRegular[] mcrs = new MultiCostRegular[nE];
	for (int e = 0; e < nE; e++) {
		mcrs[e] = (MultiCostRegular) this.getCstr(m.getMcrConstraint(e));
	}
	boolean[] priorValues = new boolean[m.getNbValues()];
	for (int a = priorValues.length - 2; a >= 0; a--) {
		priorValues[a] = true;
	}
	priorValues[priorValues.length - 1] = false;

	VarSelector<IntDomainVar> varSelector = (!scoreType.isEmpty()) ? new MCRBasedRowValueSelector(this, shiftVars, mcrs, priorValues, scoreType) : new MCRBasedRowValueSelector(this, shiftVars, mcrs, priorValues);
	return new AssignVar(varSelector, (ValSelector) varSelector);
}

public void setRestarts(int restartLimitFail, double restartGrow, Integer restartMax)
{
	if (restartMax == null) {
		this.setGeometricRestart(restartLimitFail, restartGrow);
	} else {
		this.setGeometricRestart(restartLimitFail, restartGrow, restartMax);
	}
}

}
