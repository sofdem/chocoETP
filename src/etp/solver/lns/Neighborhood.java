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

import choco.kernel.solver.branch.AbstractIntBranchingStrategy;

/**
 * Neighborhood defines how to build and to explore the search space of a problem around a solution
 * in hope to improve locally the solution
 * @author Sophie Demassey
 * @see LNSCPSolver
 */
public class Neighborhood implements Comparable {

//private Solution solution;
/** operator defines how to build the search space around a solution */
private NeighborhoodOperator operator;
/**
 * strategy defines how to explore this search space within a backtracking
 * todo: encapsulate the type of the heuristic rather than a heuristic attached to a solver
 */
private AbstractIntBranchingStrategy strategy;
/** impact is a performance indicator */
private int impact;

public Neighborhood(NeighborhoodOperator operator, AbstractIntBranchingStrategy strategy, int impact)
{
//	this.solution = solution;
	this.operator = operator;
	this.strategy = strategy;
	this.impact = impact;
}

public Neighborhood(NeighborhoodOperator operator)
{
	this(operator, null, 0);
}

/*public Solution getSolution()
{
	return solution;
}*/

public NeighborhoodOperator getOperator()
{
	return operator;
}

public AbstractIntBranchingStrategy getStrategy()
{
	return strategy;
}

/*
public void setSolution(Solution solution)
{
	this.solution = solution;
}
*/

@Override
public int compareTo(Object o)
{
	return (this.impact - ((Neighborhood) o).impact);
}

public int decreaseImpact()
{
	return --impact;
}

public int increaseImpact()
{
	return ++impact;
}

}
