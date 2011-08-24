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

package etp.solver.search;

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 02/03/11 - 12:47
 */

import choco.automaton.MultiCostRegular;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.util.iterators.DisposableIntIterator;
import choco.kernel.common.util.tools.ArrayUtils;
import choco.kernel.memory.IStateInt;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.Solver;
import choco.kernel.solver.search.ValSelector;
import choco.kernel.solver.search.integer.AbstractIntVarSelector;
import choco.kernel.solver.variables.integer.IntDomainVar;
import etp.data.EtpCounter;

import java.util.Random;

// TODO: extend to take the nb of assignment into account

/** @author Sophie Demassey */
public class MCRBasedRowValueSelector extends AbstractIntVarSelector implements ValSelector<IntDomainVar> {

/** the matrix of the variables [nbVectors][nbColumns] */
final IntDomainVar[][] sVars;
final int nCol;
final int nRow;

/** the last selected variable */
IntDomainVar selectedVar;

/** the last selected value */
int selectedVal;

/** the last considered column */
final IStateInt lastCol;

/** the number of restarts */
int lastRestart;

/** the index of the last selectedVar */
int lastVar;

/** The MultiCostRegular constraints [nbVectors] */
final MultiCostRegular[] rowConstraints;

boolean[] priorValues;
int[] starter;

final int mainDimension;
final int nbShiftDimension;
final Scores scores;
final Random randomizer;

public MCRBasedRowValueSelector(Solver s, IntDomainVar[][] sVars, MultiCostRegular[] mcrs, boolean priorValues[])
{
	this(s, sVars, mcrs, priorValues, Scores.MaxRegret_MinSP);
}

public MCRBasedRowValueSelector(Solver s, IntDomainVar[][] sVars, MultiCostRegular[] mcrs, boolean priorValues[], String scoreType)
{
	this(s, sVars, mcrs, priorValues, Scores.valueOf(scoreType));
}

public MCRBasedRowValueSelector(Solver s, IntDomainVar[][] sVars, MultiCostRegular[] mcrs, boolean priorValues[], Scores scoreType)
{
	super(s, ArrayUtils.flatten(sVars));
	this.sVars = sVars;
	this.nCol = sVars[0].length;
	this.nRow = sVars.length;
	this.lastCol = s.getEnvironment().makeInt(0);
	this.lastRestart = -1;
	this.rowConstraints = mcrs;
	this.priorValues = priorValues;
	this.lastVar = -1;
	this.starter = ArrayUtils.zeroToN(nCol);

	this.scores = scoreType;
	this.randomizer = new Random();

	this.mainDimension = 0;
	assert ((EtpCounter) mcrs[0].pi.getCounters().get(0)).isCostEqualToCounter();
	this.nbShiftDimension = 1;
	if (!scoreType.equals(Scores.MinSP)) assert ((EtpCounter) mcrs[0].pi.getCounters().get(1)).isGlobalShiftCounter();
}

private boolean justRestarted()
{
	if (lastRestart >= this.solver.getRestartCount()) {
		return false;
	}
	lastRestart = this.solver.getRestartCount();
	lastCol.set(starter[lastRestart % nCol]);
	return true;
}

/**
 * find an under-covered value and select a variable to be assigned to.
 * scan all columns starting from the last considered one in order to find an under-covered value.
 * If exists, then select the value and a variable (first or minDomain) in the column to be assigned to the value.
 * Otherwise, select a default variable and its maximum value.
 * @return the selected variable
 */
@Override
public IntDomainVar selectVar()
{
	boolean justRestarted = justRestarted();
	selectedVar = null;
	int nbTry = 0;
	while (nbTry < nCol && !findBestScore(lastCol.get(), justRestarted)) {
		if (lastCol.get() == nCol - 1) {
			lastCol.set(0);
		} else {
			lastCol.add(1);
		}
		nbTry++;
	}
	//if(selectedVar == null && ChocoLogging.getMainLogger().isLoggable(Level.FINEST))
	//	ChocoLogging.getMainLogger().finest("backtrack");
	//if (justRestarted) {
	//	System.err.println("start " + selectedVar + " " + solver.getFailCount());
	//}
	//if (selectedVar==null) {
	//	System.out.println("coucou ici");
	//}
	return selectedVar;
}

/**
 * get the selected value for the variable.
 * If the variable is the one selected by the heuristic then return the selected under-covered value,
 * otherwise return the variable maximum value.
 * @param x the variable
 * @return the selected value
 */
@Override
public int getBestVal(IntDomainVar x)
{
	return (x == selectedVar && x.canBeInstantiatedTo(selectedVal)) ? selectedVal : x.getSup();
}


public enum Scores {
	MinSP
			{
				@Override
				void init() { bestScore = new int[]{Integer.MAX_VALUE}; }

				@Override
				void selectAndUpdate(MCRBasedRowValueSelector sel, int col, int row)
				{
					bestScore = sel.scoreMinSP(col, row, bestScore);
				}
			},
	MinRegret_MinSP_MinGS
			{
				@Override
				void init() { bestScore = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}; }

				@Override
				void selectAndUpdate(MCRBasedRowValueSelector sel, int col, int row)
				{
					bestScore = sel.scoreMinRegretMinSPMinGS(col, row, bestScore);
				}
			},
	MaxRegret_MinSP
			{
				int[] score = new int[2];

				@Override
				void init() { bestScore = new int[]{0, Integer.MAX_VALUE}; }

				@Override
				void selectAndUpdate(MCRBasedRowValueSelector sel, int col, int row)
				{
					bestScore = sel.scoreMaxRegretMinSP(col, row, bestScore, score);
				}
			};
	int[] bestScore;

	abstract void init();

	abstract void selectAndUpdate(MCRBasedRowValueSelector sel, int col, int row);
}


/**
 * consider all variables in the column col (starting from the last selected row)
 * if they are all instantiated, then select nothing and return false
 * otherwise compute the scores of each un-instantiated variable and select the best one
 * @param col the column number
 * @return true iff a variable is selected
 */
private boolean findBestScore(int col, boolean justRestarted)
{
	scores.init();
	selectedVar = null;

	int row = (justRestarted) ? randomizer.nextInt(nRow - 1) : (lastVar + 1 == nRow) ? 0 : lastVar + 1;
	for (int i = 0; i < nRow; i++, row++) {
		if (row == nRow) row = 0;
		if (!sVars[row][col].isInstantiated()) {
			scores.selectAndUpdate(this, col, row);
		}
	}
	//if(ChocoLogging.getMainLogger().isLoggable(Level.FINEST)) {
	//	if(selectedVar == null)
	//		ChocoLogging.getMainLogger().finest("next col");
	//	else
	//		ChocoLogging.getMainLogger().finest(" best ! (" + col + "," + lastVar + "," + selectedVal + "):");
	//}


	return selectedVar != null;

}

/**
 * consider all variables in the column col (starting from the last selected row)
 * if they are all instantiated, then select nothing and return false
 * otherwise consider, for each un-instantiated variable, the minimal solution of the MCR constraint involving the variable
 * select the first variable whose value in the solution is prior and for which the cost of the MCR solution is minimal
 * @param col the column number
 * @return true iff a variable is selected
 */
private int[] scoreMinSP(int col, int row, int[] bestScore)
{
	MultiCostRegular cons = rowConstraints[row];
	int val = cons.getGraph().GArcs.values[cons.lastSp[col]];
	if (selectedVar == null || isPriorToSelected(val) || (isComparableToSelected(val) && bestScore[0] > cons.lastSpValue)) {
		selectedVar = sVars[row][col];
		selectedVal = val;
		lastVar = row;
		bestScore[0] = (int) cons.lastSpValue;
	}
	return bestScore;
}

/**
 * consider all variables in the column col (starting from the last selected row)
 * if they are all instantiated, then select nothing and return false
 * otherwise consider, for each un-instantiated variable, the minimal solution of the MCR constraint involving the variable
 * select the first variable whose value in the solution is prior and for which the cost of the MCR solution is minimal
 * @param col the column number
 * @return true iff a variable is selected
 */
private int[] scoreMinSP2(int col, int row, int[] bestScore)
{
	MultiCostRegular cons = rowConstraints[row];
	int val = cons.getGraph().GArcs.values[cons.lastSp[col]];
	if (selectedVar == null || isPriorToSelected(val) || (isComparableToSelected(val) && bestScore[0] > cons.lastSpValue)) {
		selectedVar = sVars[row][col];
		selectedVal = val;
		lastVar = row;
		bestScore[0] = (int) cons.lastSpValue;
		int nbAssignment = cons.getMinPathCost(nbShiftDimension);
		int penaltyAssignment = cons.pi.getCounters().get(1).bounds().penalty(nbAssignment);
	}
	return bestScore;
}


private int[] scoreMinRegretMinSPMinGS(int col, int row, int[] bestScore)
{
	MultiCostRegular cons = rowConstraints[row];
	updateMCR(cons);
	DisposableIntIterator it = sVars[row][col].getDomain().getIterator();
	int spLength = cons.getMinPathCost(mainDimension);
	int nbAssignment = cons.getMinPathCost(nbShiftDimension);
	while (it.hasNext()) {
		int val = it.next();
		if (isPrior(val)) {
			int sp = cons.getMinMaxPathCostForAssignment(col, val, mainDimension)[0];
			int regret = sp - spLength;
			if (bestScore[0] > regret || (bestScore[0] == regret && (bestScore[1] > sp || (bestScore[1] == sp && bestScore[2] > nbAssignment)))) {
				//if(ChocoLogging.getMainLogger().isLoggable(Level.FINEST)) {
				//	if (bestScore[0] > regret) ChocoLogging.getMainLogger().finest(" reg=" + regret);
				//	else if (bestScore[1] > sp) ChocoLogging.getMainLogger().finest(" sp=" + sp);
				//	else if (bestScore[2] > nbAssignment) ChocoLogging.getMainLogger().finest(" ass=" + nbAssignment);
				//}
				bestScore[0] = regret;
				bestScore[1] = sp;
				bestScore[2] = nbAssignment;
				selectedVar = sVars[row][col];
				selectedVal = val;
				lastVar = row;
			}
		}
	}
	it.dispose();
	return bestScore;
}

private int[] scoreMaxRegretMinSP(int col, int row, int[] bestScore, int[] score)
{
	int tmpSelectedVal = selectedVal;
	selectedVal = -1;
	score = this.computeRegretSP(col, row, score);
	if (selectedVal < 0) {
		throw new RuntimeException("no path is the smallest one !");
	}
	if (bestScore[0] < score[0] || (bestScore[0] == score[0] && (bestScore[1] > score[1]))) {
		//if(ChocoLogging.getMainLogger().isLoggable(Level.FINEST)) {
		//	if (bestScore[0] < score[0]) ChocoLogging.getMainLogger().finest(" reg=" + score[0]);
		//	else if (bestScore[1] > score[1]) ChocoLogging.getMainLogger().finest(" sp=" + score[1]);
		//}
		bestScore[0] = score[0];
		bestScore[1] = score[1];
		selectedVar = sVars[row][col];
		lastVar = row;
	} else {
		selectedVal = tmpSelectedVal;
	}
	return bestScore;
}

private int[] computeRegretSP(int col, int row, int[] score)
{
	MultiCostRegular cons = rowConstraints[row];
	updateMCR(cons);
	DisposableIntIterator it = sVars[row][col].getDomain().getIterator();
	score[1] = cons.getMinPathCost(mainDimension);
	score[0] = Integer.MAX_VALUE;
	while (it.hasNext() && score[0] > 0) {
		int val = it.next();
		int spSupportingVal = cons.getMinMaxPathCostForAssignment(col, val, mainDimension)[0];
		if (spSupportingVal < score[1]) {
			throw new RuntimeException("paths cannot be smaller than the smallest one !");
		} else if (spSupportingVal == score[1]) {
			if (selectedVal < 0) {
				selectedVal = val;
			} else {
				if (isComparableOrPriorToSelected(val)) {
					selectedVal = val;
				}
				score[0] = 0;
			}
		} else {
			score[0] = Math.min(score[0], spSupportingVal - score[1]);
		}
	}
	it.dispose();
	return score;
}

private int[] computeRegretSPAndAssignment(int col, int row, int[] score)
{
	MultiCostRegular cons = rowConstraints[row];
	updateMCR(cons);

	DisposableIntIterator it = sVars[row][col].getDomain().getIterator();
	score[0] = Integer.MAX_VALUE;   // regret
	score[1] = cons.getMinPathCost(mainDimension);  // SP length
	score[2] = cons.getMinPathCost(nbShiftDimension);   // min nb Assignment
	while (it.hasNext() && score[0] > 0) {
		int val = it.next();
		int spSupportingVal = cons.getMinMaxPathCostForAssignment(col, val, mainDimension)[0];
		int minNbAssignmentSupportingVal = cons.getMinMaxPathCostForAssignment(col, val, nbShiftDimension)[0];
		int penaltyAssignment = cons.pi.getCounters().get(1).bounds().penalty(minNbAssignmentSupportingVal);


		int regret = (spSupportingVal + minNbAssignmentSupportingVal) - (score[1] + score[2]);
		if (spSupportingVal < score[1]) {
			throw new RuntimeException("paths cannot be smaller than the smallest one !");
		} else if (spSupportingVal == score[1]) {
			if (selectedVal < 0) {
				selectedVal = val;
			} else {
				if (isComparableOrPriorToSelected(val)) {
					selectedVal = val;
				}
				score[0] = 0;
			}
		} else {
			score[0] = Math.min(score[0], spSupportingVal - score[1]);
		}
	}
	it.dispose();
	return score;
}


private boolean isPrior(int val) { return priorValues[val]; }

private boolean isPriorToSelected(int val) { return !isPrior(selectedVal) && isPrior(val); }

private boolean isComparableToSelected(int val) { return isPrior(val) == isPrior(selectedVal); }

private boolean isComparableOrPriorToSelected(int val) { return isPrior(val) || !isPrior(selectedVal); }

private void updateMCR(MultiCostRegular cons)
{
	try {
		cons.rebuildCostRegInfo();
	} catch (ContradictionException e) {
		ChocoLogging.getSearchLogger().severe("all constraint should be consistent before branching");
	}
}

}
