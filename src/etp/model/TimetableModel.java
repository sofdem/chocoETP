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

package etp.model;

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: Nov 15, 2010 - 12:12:42 PM
 */

import choco.Choco;
import choco.Options;
import choco.automaton.FA.CostAutomaton;
import choco.automaton.FA.FiniteAutomaton;
import choco.automaton.FA.utils.ICounter;
import choco.automaton.MultiCostRegularManager;
import choco.cp.model.CPModel;
import choco.cp.model.managers.constraints.global.FastRegularManager;
import choco.kernel.common.util.tools.ArrayUtils;
import choco.kernel.model.constraints.ComponentConstraint;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;
import etp.data.EtpCounter;
import etp.data.components.Cover;
import gnu.trove.TIntArrayList;

import java.util.ArrayList;
import java.util.List;


/** @author Sophie Demassey */
public class TimetableModel extends CPModel implements EtpModel {

private final int nT;
private final int nA;
private final int nE;

private IntegerVariable[][] shiftVars;
private IntegerVariable[][] coverVars;
private IntegerVariable[][] counterVars;
private IntegerVariable[] objVars;
private IntegerVariable objVar;

private IntegerVariable[] counterGlobalShiftVars;
private IntegerExpressionVariable coverGlobalShiftExpr;

private Constraint[] mcrConstraints;


public IntegerVariable[][] getShiftVars()
{
	return shiftVars;
}

public IntegerVariable[][] getCoverVars()
{
	return coverVars;
}

public IntegerVariable[][] getCounterVars()
{
	return counterVars;
}

public IntegerVariable[] getObjVars()
{
	return objVars;
}

public IntegerVariable getObjVar()
{
	return objVar;
}

public int getNbValues()
{
	return nA;
}


public Constraint getMcrConstraint(int e)
{
	return mcrConstraints[e];
}

public TimetableModel(int nPeriods, int nActivities, int nEmployees)
{
	super();
	nT = nPeriods;
	nA = nActivities;
	nE = nEmployees;
	shiftVars = Choco.makeIntVarArray("X", nE, nT, 0, nA - 1, "cp:enum");
	counterVars = new IntegerVariable[nE][];
	objVars = Choco.makeIntVarArray("O", nE, Choco.MIN_LOWER_BOUND, Choco.MAX_UPPER_BOUND, "cp:bound");
	objVar = null;
	mcrConstraints = new Constraint[nE];
	counterGlobalShiftVars = new IntegerVariable[nE];


}

/**
 * ********************************************************************************************************************
 *
 * ASSIGNMENTS - forbidden or mandatory (hard) activity assignment to an employee/time pair
 *
 * ********************************************************************************************************************
 */

@Override
public void postForbiddenAssignment(int e, int t, int a)
{
	this.addConstraint(Choco.neq(shiftVars[e][t], a));
}

@Override
public void postMandatoryAssignment(int e, int t, int a)
{
	this.addConstraint(Choco.eq(shiftVars[e][t], a));
}


/**
 * ********************************************************************************************************************
 *
 * COVERS - global and partial
 *
 * ********************************************************************************************************************
 */


public enum OptionCover {
	GCC_VAR
			{
				void post(TimetableModel m, Cover cover, IntegerVariable[][] sVars)
				{
					m.postCoverByGccVar(sVars, cover);
				}
			},
	GCC_CST_AC
			{
				void post(TimetableModel m, Cover cover, IntegerVariable[][] sVars)
				{
					m.postCoverByGccCst(sVars, cover, "cp:ac");
				}
			},
	GCC_CST_BC
			{
				void post(TimetableModel m, Cover cover, IntegerVariable[][] sVars)
				{
					m.postCoverByGccCst(sVars, cover, "cp:bc");
				}
			},
	NO
			{
				void post(TimetableModel m, Cover cover, IntegerVariable[][] sVars) {}
			};

	abstract void post(TimetableModel m, Cover cover, IntegerVariable[][] sVars);
}

@Override
public void postGlobalCover(Cover cover, String option)
{
	this.postCover(null, cover, option);
}

@Override
public void postPartialCover(int[] employees, Cover cover, String option)
{
	this.postCover(employees, cover, option);
}

private void postCover(int[] employees, Cover cover, String option)
{
	try {
		OptionCover implem = OptionCover.valueOf(option);
		implem.post(this, cover, ArrayUtils.transpose(selectShiftVars(employees)));
	} catch (IllegalArgumentException e) {
		LOGGER.warning("invalid option '" + option + "': no cover constraint posted");
	}
}

private IntegerVariable[][] selectShiftVars(int[] employees)
{
	if (employees == null || employees.length == nE) return shiftVars;

	IntegerVariable[][] sVars = new IntegerVariable[employees.length][nT];
	int i = 0;
	for (int e : employees) {
		sVars[i] = shiftVars[e];
		i++;
	}
	return sVars;
}

/**
 * generate a set of cover variables and constraints and add them to the CP model.
 * @param cover the cover to model
 */
private void postCoverByGccVar(IntegerVariable[][] sVars, Cover cover)
{
	IntegerVariable[][] cVars = new IntegerVariable[nT][nA];
	for (int t = 0; t < nT; t++) {
		for (int a = 0; a < nA; a++) {
			cVars[t][a] = Choco.makeIntVar("c" + cover.getLabel() + "_t" + t + "a" + a, cover.getLB(t, a), cover.getUB(t, a), "cp:bound");
		}
		this.addConstraint(Choco.globalCardinality(sVars[t], cVars[t], 0));
	}

	if (sVars[0].length == nE) {
		assert coverVars == null : "global cover variables already created";
		coverVars = cVars;
		coverGlobalShiftExpr = Choco.sum(ArrayUtils.flattenSubMatrix(0, nT, 0, nA - 1, coverVars));
	}
}

/**
 * generate a set of cover variables and constraints and add them to the CP model.
 * @param cover the cover to model
 */
private void postCoverByGccCst(IntegerVariable[][] sVars, Cover cover, String consistencyLevel)
{
	for (int t = 0; t < nT; t++) {
		this.addConstraint(Choco.globalCardinality(consistencyLevel, sVars[t], cover.getLB(t), cover.getUB(t), 0));
	}
	if (sVars[0].length == nE) { // todo compute in Data
		int lb = 0, ub = 0;
		for (int t = 0; t < nT; t++) {
			for (int a = nA - 1; a >= 0; a--) {
				lb += cover.getLB(t, a);
				ub += cover.getUB(t, a);
			}
		}
		coverGlobalShiftExpr = Choco.makeIntVar("coverGS", lb, ub, Options.V_NO_DECISION);
	}
}

/**
 * ********************************************************************************************************************
 *
 * OBJECTIVE
 *
 * ********************************************************************************************************************
 */

public enum OptionObj {
	SUM_SOFT
			{
				void post(TimetableModel m, int lb, int ub) { m.postObjectiveSumSoft(lb, ub); }
			},
	NO
			{
				void post(TimetableModel m, int lb, int ub) {}
			};

	abstract void post(TimetableModel m, int lb, int ub);
}


/**
 * generate the objective according to a given configuration and add it to the CP model.
 * objective is set to the sum of the shift model objective variables
 */
@Override
public void postObjective(int lb, int ub, String option)
{
	try {
		OptionObj implem = OptionObj.valueOf(option);
		implem.post(this, lb, ub);
	} catch (IllegalArgumentException e) {
		LOGGER.warning("invalid option '" + option + "': no objective posted");
	}
}

/**
 * generate the objective according to a given configuration and add it to the CP model.
 * objective is set to the sum of the shift model objective variables
 */
private void postObjectiveSumSoft(int lb, int ub)
{
	objVar = Choco.makeIntVar("obj", lb, ub, "cp:bound");
	this.addConstraint(Choco.eq(objVar, Choco.sum(objVars)));
	//LOGGER.finer("set objective [ " + p.objectiveLB + " , " + p.objectiveUB + " ]");
}


/**
 * ********************************************************************************************************************
 *
 * AUTOMATA
 *
 * ********************************************************************************************************************
 */

public enum OptionRules {
	REG
			{
				void post(TimetableModel m, int e, FiniteAutomaton automaton) { m.postRegularConstraint(e, automaton); }
			},
	MCR
			{
				void post(TimetableModel m, int e, FiniteAutomaton automaton)
				{
					m.postMultiCostRegularConstraint(e, (CostAutomaton) automaton);
				}
			},
	NO
			{
				void post(TimetableModel m, int e, FiniteAutomaton automaton) {}
			};

	abstract void post(TimetableModel m, int e, FiniteAutomaton automaton);
}


/**
 * generate the objective according to a given configuration and add it to the CP model.
 * objective is set to the sum of the shift model objective variables
 */
@Override
public void postRules(int e, FiniteAutomaton automaton, String option)
{
	try {
		OptionRules implem = OptionRules.valueOf(option);
		implem.post(this, e, automaton);
	} catch (IllegalArgumentException ex) {
		LOGGER.warning("invalid option '" + option + "': no employee rules posted");
	}
}

/**
 * generate the Regular constraint and add it to the CP model.
 * @param automaton the automaton
 */
public void postRegularConstraint(int e, FiniteAutomaton automaton)
{
	assert false : "le manager n'a pas été recopié et attends pour un choco automaton";
	this.addConstraint(new ComponentConstraint(FastRegularManager.class, automaton, shiftVars[e]));
}

/**
 * generate the Regular constraint and add it to the CP model.
 * @param automaton the automaton
 */
public void postMultiCostRegularConstraint(int e, CostAutomaton automaton)
{
	IntegerExpressionVariable expr = this.makeCounterVariables(e, automaton.getCounters());
	if (expr != null) {
		this.addConstraint(Choco.eq(objVars[e], expr));
	}
	mcrConstraints[e] = new ComponentConstraint(MultiCostRegularManager.class, new Object[]{shiftVars[e].length, automaton}, ArrayUtils.append(shiftVars[e], counterVars[e]));
	//Choco.multiCostRegular(counterVars[e], shiftVars[e], automaton);
	this.addConstraint(mcrConstraints[e]);
}

/**
 * generate the counter variables, the counter violation variables and their relation constraints and add them to the model.
 * for each counter of the automaton: generate a hard bound variable (C in [Lhard, Uhard]), and if need be,
 * a violation cost variable (S in [0,MaxCost]) and the relation constraint S = max(0, Lsoft-C, C-Usoft).
 * specific counters are also classified by type.
 * @param counters the list of counters
 */
public IntegerExpressionVariable makeCounterVariables(int e, List<ICounter> counters)
{
	counterVars[e] = new IntegerVariable[counters.size()];
	ArrayList<IntegerVariable> oVars = new ArrayList<IntegerVariable>(counters.size());
	TIntArrayList oCoefs = new TIntArrayList(counters.size());

	int nc = 0;
	//LOGGER.finer("counter variables:");
	for (ICounter ic : counters) {
		EtpCounter c = (EtpCounter) ic;
		IntegerVariable cVar, sVar;

		if (c.isCostEqualToCounter()) {
			cVar = Choco.makeIntVar("c" + e + "_" + c.getName(), c.getMin(), c.getMax(), "cp:bound", Options.V_NO_DECISION);
			sVar = cVar;
		} else if (c.isCostLinearInCounter()) {
			System.err.println("more efficient to transform the linear penalty on counter in order to get cost=counter");
			cVar = Choco.makeIntVar("c" + e + "_" + c.getName(), c.getMin(), c.getMax(), "cp:bound", Options.V_NO_DECISION);
			sVar = Choco.makeIntVar("s" + e + "_" + c.getName(), 0, c.getMaxPenaltyValue(), "cp:bound", Options.V_NO_DECISION);
			this.addConstraint(Choco.eq(sVar, Choco.plus(c.bounds().getConstant(), Choco.mult(c.bounds().getFactor(), cVar))));
		} else {
			cVar = Choco.makeIntVar("c" + e + "_" + c.getName(), c.getMin(), c.getMax(), "cp:enum", Options.V_NO_DECISION);
			sVar = Choco.makeIntVar("s" + e + "_" + c.getName(), 0, c.getMaxPenaltyValue(), "cp:enum", Options.V_NO_DECISION);
			List<int[]> feasTuple = c.makePenaltyRelationTable();
			if (feasTuple.size() > 100) {
				LOGGER.warning("counter/soft relation on more than 100 pairs : " + cVar);
			}
			this.addConstraint(Choco.feasPairAC(cVar, sVar, feasTuple));

			if (c.isGlobalShiftCounter()) {
				counterGlobalShiftVars[e] = cVar;
			}


		}

		counterVars[e][nc++] = cVar;
		if (c.getObjectiveCoefficient() != 0) {
			oVars.add(sVar);
			oCoefs.add(c.getObjectiveCoefficient());
		}
	}
	return (oCoefs.isEmpty()) ? null : Choco.scalar(oVars.toArray(new IntegerVariable[oVars.size()]), oCoefs.toNativeArray());
}


/**
 * ********************************************************************************************************************
 *
 * LINKING
 *
 * ********************************************************************************************************************
 */

public enum OptionLinking {
	GS
			{
				void post(TimetableModel m) { m.postCoverCounterGlobalShiftLinkingConstraints(); }
			};

	abstract void post(TimetableModel m);
}


/** generate the redundant constraints linking counter and cover variables */
@Override
public void postLinkingConstraints(String option)
{
	try {
		OptionLinking implem = OptionLinking.valueOf(option);
		implem.post(this);
	} catch (IllegalArgumentException e) {
		LOGGER.warning("invalid option '" + option + "': linking constraint not posted");
	}
}


/** generate cover[all shift types]-counter[all shift types] linking constraints and add them to the CP model. */
public void postCoverCounterGlobalShiftLinkingConstraints()
{
	for (IntegerVariable cVar : counterGlobalShiftVars) {
		if (cVar == null) return;
	}

	if (coverGlobalShiftExpr != null) {
		this.addConstraint(Choco.eq(coverGlobalShiftExpr, Choco.sum(counterGlobalShiftVars)));
		LOGGER.finer("add cover-counters linking constraint on the total number of shifts");
	}
}


}
