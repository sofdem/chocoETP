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

import choco.cp.solver.search.BranchingFactory;
import choco.cp.solver.search.integer.branching.ImpactBasedBranching;
import choco.kernel.solver.branch.AbstractIntBranchingStrategy;
import etp.model.EtpModel;
import etp.output.ETPOutput;

import java.util.Properties;

/** @author Sophie Demassey */
public abstract class EtpSolverBuilder {

protected EtpSolver solver;
protected final SolverProperties properties;
protected EtpModel model;

protected class SolverProperties {
	boolean minimize;
	boolean lns;

	boolean restart;
	Integer restartLimitFail;
	double restartGrow;
	Integer restartMax;

	Integer limitBk;
	Integer limitTime;
	Integer limitFail;
	String heuristic;

	public SolverProperties(Properties properties)
	{
		this.minimize = Boolean.parseBoolean(properties.getProperty("solver.minimize"));
		this.lns = Boolean.parseBoolean(properties.getProperty("solver.lns"));

		this.restart = Boolean.parseBoolean(properties.getProperty("solver.restart"));
		this.restartLimitFail = getInteger(properties.getProperty("solver.restart.limit.fail"));
		this.restartGrow = getDouble(1, properties.getProperty("solver.restart.grow"));
		this.restartMax = getInteger(properties.getProperty("solver.restart.max"));

		this.limitBk = getInteger(properties.getProperty("solver.limit.backtrack"));
		this.limitTime = getInteger(properties.getProperty("solver.limit.time"));
		this.limitFail = getInteger(properties.getProperty("solver.limit.fail"));

		this.heuristic = properties.getProperty("search.heuristic");
		//if (heuristic.equals("MCR")) {
		//	heuristic += " " + properties.getProperty("search.heuristic.mcr.scores");
		//}
	}

	private Integer getInteger(String s)
	{
		return (s == null) ? null : Integer.parseInt(s);
	}

	private int getInteger(int defaultValue, String s)
	{
		return (s == null) ? defaultValue : Integer.parseInt(s);
	}

	private double getDouble(double defaultValue, String s)
	{
		return (s == null) ? defaultValue : Double.parseDouble(s);
	}

	public String[] getHeuristicOptions()
	{
		return (heuristic == null) ? null : heuristic.split(" ");
	}
}

public EtpSolverBuilder(Properties properties)
{
	this.properties = new SolverProperties(properties);
}

public EtpSolver getSolver() { return solver; }

public abstract void buildSolver(EtpModel model, ETPOutput writer);

public void setLimits()
{
	if (properties.limitTime != null) {
		solver.setTimeLimit(properties.limitTime);
	} else if (properties.limitBk != null) {
		solver.setBackTrackLimit(properties.limitBk);
	} else if (properties.limitFail != null) {
		solver.setFailLimit(properties.limitFail);
	}
}

public void setRestarts()
{
	if (!properties.restart || properties.restartLimitFail == null) return;
	solver.setRestarts(properties.restartLimitFail, properties.restartGrow, properties.restartMax);
}

public enum HeuristicOption {
	RAND
			{
				AbstractIntBranchingStrategy get(EtpSolver solver)
				{
					return BranchingFactory.randomIntSearch(solver, 0);
				}
			},
	RAND_SEED_VAR
			{
				AbstractIntBranchingStrategy get(EtpSolver solver)
				{
					return BranchingFactory.randomIntSearch(solver, System.currentTimeMillis());
				}
			},
	IMPACT
			{
				AbstractIntBranchingStrategy get(EtpSolver solver) { return new ImpactBasedBranching(solver); }
			},
	MINDOM_MINVAL
			{
				AbstractIntBranchingStrategy get(EtpSolver solver) { return BranchingFactory.minDomMinVal(solver); }
			};

	abstract AbstractIntBranchingStrategy get(EtpSolver solver);

	public static String valuestoString()
	{
		String s = "";
		for (HeuristicOption o : values()) s += o.name() + " ";
		return s;
	}
}

public AbstractIntBranchingStrategy getBranchingStrategy(String option)
{
	try {
		return HeuristicOption.valueOf(option).get(solver);
	} catch (IllegalArgumentException e) {
		return solver.getBranchingStrategy(option);
	}
}

public void setHeuristic()
{
	solver.clearGoals();
	for (String heuristic : properties.getHeuristicOptions()) {
		AbstractIntBranchingStrategy strategy = getBranchingStrategy(heuristic);
		if (strategy != null) {
			solver.addGoal(strategy);
		} else {
			solver.LOGGER.warning("invalid branching strategy '" + heuristic + ". Valid options are " + HeuristicOption.valuestoString());
		}
	}
}

}
