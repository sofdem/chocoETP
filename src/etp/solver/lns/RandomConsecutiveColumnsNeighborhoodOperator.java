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

package etp.solver.lns;

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.solver.Solution;
import choco.kernel.solver.Solver;
import choco.kernel.solver.variables.integer.IntDomainVar;
import etp.solver.TimetableCPSolver;

import java.util.Random;

/**
 * A neighborhood operator which unfix consecutive columns of variables randomly
 * @author Sophie Demassey
 */
public class RandomConsecutiveColumnsNeighborhoodOperator implements NeighborhoodOperator {
protected Random random;
protected int minSize;
protected int maxSize;
int firstCol = -1;

/**
 * Constructs with a fixed seed.
 * @param nbRelaxedCols number of columns to let free
 */
public RandomConsecutiveColumnsNeighborhoodOperator(int nbRelaxedCols)
{
	this(nbRelaxedCols, 0);
}

/**
 * Constructs with a specified seed.
 * @param nbRelaxedCols number of columns to let free
 * @param seed          random seed
 */
public RandomConsecutiveColumnsNeighborhoodOperator(int nbRelaxedCols, long seed)
{
	this(nbRelaxedCols, nbRelaxedCols, seed);
}

/**
 * Constructs with a specified seed and a variable neighborhood size.
 * @param minNbRelaxedCols minimum number of columns to let free
 * @param maxNbRelaxedCols maximum number of columns to let free
 * @param seed             random seed
 */
public RandomConsecutiveColumnsNeighborhoodOperator(int minNbRelaxedCols, int maxNbRelaxedCols, long seed)
{
	random = new Random(seed);
	this.minSize = minNbRelaxedCols;
	this.maxSize = maxNbRelaxedCols;
}

/**
 * restrict the search space around the solution by selecting nbRelaxedCols consecutive columns randomly to let free
 * and by fixing all other column of variables to their value in solution
 * @param solution the solution to build the neighborhood around
 * @return true iff the search space is actually shrunken
 */
@Override
public boolean restrictNeighborhood(Solution solution)
{
	TimetableCPSolver solver = (TimetableCPSolver) solution.getSolver();
	int nbRelaxedCols = (minSize == maxSize) ? minSize : random.nextInt(maxSize - minSize + 1) + minSize;
	int nbCols = solver.getShiftVars()[0].length;
//	int firstCol = random.nextInt(nbCols);
	firstCol++;
	if (firstCol >= nbCols) { firstCol = 0; }

	ChocoLogging.getMainLogger().info("LNS " + nbRelaxedCols + " consecutive columns: " + firstCol + "-" + (firstCol + nbRelaxedCols - 1));
	boolean b = false;
	for (int i = 0; i < firstCol; i++) {
		for (int j = 0; j < solver.getShiftVars().length; j++) {
			b |= setVar(solution, solver.getShiftVars()[j][i], solver);
		}
	}
	for (int i = firstCol + nbRelaxedCols; i < nbCols; i++) {
		for (int j = 0; j < solver.getShiftVars().length; j++) {
			b |= setVar(solution, solver.getShiftVars()[j][i], solver);
		}
	}
	return b;
}

private boolean setVar(Solution solution, IntDomainVar var, Solver solver)
{
	if (!var.isInstantiated()) {
		int val = solution.getIntValue(solver.getIntVarIndex(var));
		if (val != Solution.NULL) {
			solver.post(solver.eq(var, val));
			return true;
		}
	}
	return false;

}

}
