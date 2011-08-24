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

import choco.cp.solver.CPSolver;
import choco.cp.solver.configure.RestartFactory;
import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.solver.Configuration;
import choco.kernel.solver.ResolutionPolicy;
import choco.kernel.solver.Solution;
import choco.kernel.solver.Solver;
import choco.kernel.solver.branch.AbstractIntBranchingStrategy;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.search.limit.Limit;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.kernel.solver.variables.integer.IntVar;
import etp.output.ETPOutput;
import etp.solver.TimetableCPSolver;

import java.util.*;

/**
 * A Large Neighborhood Search approach encapsulating a CP solver.
 * First, the CP solver computes an initial set of solutions.
 * Then, it is re-used to explore several neighborhoods around these solutions, in hope to locally improve these solutions.
 * The neighborhood of a solution is obtained by restricting the search space around the solution
 * e.g. by fixing some of the variables to their values in the solution and by relaxing the others.
 * todo 1 make LNSCPSolver implement Solver and remove the inheritance from CPSolver: aggregate the statistics of the encapsulated CPSolver
 * OR todo 2 make LNS a BranchingStrategy with restarts
 * @author Sophie Demassey
 */
public class LNSCPSolver extends TimetableCPSolver {

CPSolver solver;
int incumbent;
int objectiveLB;
Collection<Neighborhood> neighborhoods;
Collection<Solution> solutions; // todo SolutionPool
AbstractIntBranchingStrategy defaultBranchingStrategy;
//LNSCPConfiguration lnsConfiguration;


public LNSCPSolver(ETPOutput writer)
{
	super(writer);
	solver = this;  // todo delegate rather than inherits
	neighborhoods = new PriorityQueue<Neighborhood>();
	solutions = new ArrayList<Solution>();
	incumbent = Integer.MAX_VALUE;
	objectiveLB = 0;
}

/**
 * launch the large neighborhood search
 * @return FALSE if infeasibility is proved, TRUE if at least one solution is found, null otherwise
 */
@Override
public Boolean solve()
{
	solver.setObjective(objVar);
	objectiveLB = objVar.getVal();
	Boolean first = initialSearch();
	if (Boolean.TRUE != first) {
		LOGGER.warning("no solution found");
		return first;
	}
	if (objectiveLB == incumbent) return true;
	searchNeighborhoods();
	return true;
}

@Override
public AbstractIntBranchingStrategy getBranchingStrategy(String option)
{
	AbstractIntBranchingStrategy strategy = super.getBranchingStrategy(option);
	if (defaultBranchingStrategy == null) {
		defaultBranchingStrategy = strategy;
	}
	return strategy;
}

/**
 * add a neighborhood operator to apply to the solutions
 * @param operator the operator type to build the neighborhood
 * @param strategy the branching heuristic to explore the neighborhood // todo branching type rather than branching object
 * @param impact   the number of runs the operator will apply without improvement
 */
public void addNeighborhood(NeighborhoodOperator operator, AbstractIntBranchingStrategy strategy, int impact)
{
	addNeighborhood(new Neighborhood(operator, strategy, impact));
}

/**
 * add a neighborhood operator to apply to the solutions
 * using the default branching strategy and a default run number of 5
 * @param operator the operator type
 */
public void addNeighborhood(NeighborhoodOperator operator)
{
	addNeighborhood(new Neighborhood(operator, null, 5));
}

/**
 * add a neighborhood operator to apply to the solutions
 * @param neighborhood the operator
 */
public void addNeighborhood(Neighborhood neighborhood)
{
	neighborhoods.add(neighborhood);
}

/**
 * Initial step of LNS:
 * compute a first set of feasible solutions by launching a limited branch-and-bound on the solver.
 * First, the root of the search tree is duplicated thanks to a first empty branching (solver.worldPush())
 * and the solution process of the initial step is launched from the duplicated root node.
 * After this step, a backtrack to the initial root node (rootWorldIndex) will then ensure
 * the search tree and the solver to be clean for re-solving in the remaining steps.
 * @return FALSE if infeasibility is proved, TRUE if at least one solution is found, null otherwise
 */
private Boolean initialSearch()
{
	solver.getConfiguration().putEnum(Configuration.RESOLUTION_POLICY, ResolutionPolicy.SATISFACTION);
	Limit limit = Limit.FAIL; //lnsConfiguration.readEnum(LNSCPConfiguration.LNS_INIT_SEARCH_LIMIT, Limit.class);
	solver.getConfiguration().putEnum(Configuration.SEARCH_LIMIT, limit);
	solver.getConfiguration().putInt(Configuration.SEARCH_LIMIT_BOUND, 10000); //lnsConfiguration.readInt(LNSCPConfiguration.LNS_INIT_SEARCH_LIMIT_BOUND));


	solver.worldPush();
	int rootWorldIndex = solver.getWorldIndex();

	Boolean first = solver.solve(false);
	if (Boolean.TRUE != first) {
		return first;
	}

	SConstraint objectiveCut = null;
	do {
		solutions.add(solver.getSearchStrategy().getSolutionPool().getBestSolution());
		this.printRuntimeStatistics();
		this.solutionToFile();
		ChocoLogging.flushLogs();
		incumbent = solver.getObjectiveValue().intValue();
		if (incumbent > objectiveLB) {
			if (objectiveCut != null) {
				solver.eraseConstraint(objectiveCut);
			}
			objectiveCut = solver.lt((IntDomainVar) solver.getObjective(), incumbent);
			solver.postCut(objectiveCut);
		}
	} while (incumbent > objectiveLB && Boolean.TRUE == solver.nextSolution());
	if (objectiveCut != null) {
		solver.eraseConstraint(objectiveCut);
	}
	solver.worldPopUntil(rootWorldIndex);
	ChocoLogging.flushLogs();

	return Boolean.TRUE;
}

/**
 * LNS main loop:
 * the neighborhood operators are applied in turn to the solutions in the pool
 * if incumbent is improved, then the impact of the operator is incremented and the new improving solution is added to the pool
 * otherwise the impact of the operator is decremented.
 * The operator is removed from the list when its impact becomes negative.
 * The loop stops after a given number of runs or when the operator list becomes empty.
 * @return TRUE if the initial incumbent is improved, and FALSE otherwise
 */
private Boolean searchNeighborhoods()
{
	int nbLNSRuns = 200; // configuration.readInt(LNSCPConfiguration.LNS_RUN_LIMIT_NUMBER);

	Queue<Solution> newSQueue = new ArrayDeque<Solution>();
	while (!neighborhoods.isEmpty() && nbLNSRuns > 0) {
		Iterator<Neighborhood> it = neighborhoods.iterator();
		while (it.hasNext()) {
			Neighborhood neighborhood = it.next();
			Boolean improve = false;
			for (Solution solution : solutions) {
				if (Boolean.TRUE == searchNeighborhood(neighborhood, solution, incumbent)) {
					improve = true;
					newSQueue.add(solver.getSearchStrategy().getSolutionPool().getBestSolution());
					if (incumbent == objectiveLB) return true;
				}
			}
			if (!improve) {
				if (neighborhood.decreaseImpact() < 0) {
					it.remove();
				}
			} else {
				neighborhood.increaseImpact();
			}
		}
		while (!newSQueue.isEmpty()) {
			solutions.add(newSQueue.poll());
		}
		nbLNSRuns--;
	}
	return true;
}

/**
 * Explore one neighborhood defined by an operator applied to a solution:
 * the search space of the solver is restricted around the solution by the neighborhood operator,
 * and the objective is bounded by the value to improve upon.
 * The restricted search space is then explored by limited backtracking.
 * @param neighborhood the neighborhood operator
 * @param solution     the solution around which the neighborhood is explored
 * @param objToImprove the objective value to improve upon (ex: incumbent or solution objective value)
 * @return FALSE if infeasibility is proved, TRUE if one improving solution is found, null otherwise
 */
public Boolean searchNeighborhood(Neighborhood neighborhood, Solution solution, int objToImprove)
{
	Solver.LOGGER.info("\n START Neighborhood : try to improve upon " + objToImprove + "\n");

	int rootWorldIndex = solver.getWorldIndex();
	solver.worldPush();

	RestartFactory.cancelRestarts(solver);
	Limit limit = Limit.FAIL; //lnsConfiguration.readEnum(LNSCPConfiguration.LNS_NEIGHBORHOOD_SEARCH_LIMIT, Limit.class);
	solver.getConfiguration().putEnum(Configuration.SEARCH_LIMIT, limit);
	solver.getConfiguration().putInt(Configuration.SEARCH_LIMIT_BOUND, 200); //lnsConfiguration.readInt(LNSCPConfiguration.LNS_NEIGHBORHOOD_SEARCH_LIMIT_BOUND));
	solver.resetSearchStrategy();
	solver.clearGoals();
	if (neighborhood.getStrategy() != null) {
		solver.addGoal(neighborhood.getStrategy());
	} else if (defaultBranchingStrategy != null) {
		solver.addGoal(defaultBranchingStrategy);
		//	solver.addGoal(BranchingFactory.minDomMinVal(solver));
	}

	solver.post(solver.lt((IntVar) solver.getObjective(), objToImprove));

	neighborhood.getOperator().restrictNeighborhood(solution);

	Boolean ok = solver.solve(false);
	if (Boolean.TRUE == ok && incumbent >= solver.getObjectiveValue().intValue()) {
		incumbent = solver.getObjectiveValue().intValue();
		this.printRuntimeStatistics();
		this.solutionToFile();
		ChocoLogging.flushLogs();
	}
	ChocoLogging.flushLogs();
	solver.worldPopUntil(rootWorldIndex);

	return ok;
}

}