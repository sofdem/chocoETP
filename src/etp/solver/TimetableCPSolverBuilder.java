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

import etp.model.EtpModel;
import etp.output.ETPOutput;
import etp.solver.lns.LNSCPSolver;
import etp.solver.lns.Neighborhood;
import etp.solver.lns.RandomConsecutiveColumnsNeighborhoodOperator;

import java.util.Properties;

/** @author Sophie Demassey */
public class TimetableCPSolverBuilder extends EtpSolverBuilder {

public TimetableCPSolverBuilder(Properties properties)
{
	super(properties);
}

@Override
public void buildSolver(EtpModel model, ETPOutput writer)
{
	if (properties.lns) {
		this.buildLNSSolver(model, writer);
	} else {
		this.buildCPSolver(model, writer);
	}
}

protected void buildLNSSolver(EtpModel model, ETPOutput writer)
{
	solver = new LNSCPSolver(writer);
	solver.read(model);
	this.setLimits();
	this.setRestarts();
	this.setHeuristic();
	((LNSCPSolver) solver).addNeighborhood(new Neighborhood(new RandomConsecutiveColumnsNeighborhoodOperator(2), null, 5));
	((LNSCPSolver) solver).addNeighborhood(new Neighborhood(new RandomConsecutiveColumnsNeighborhoodOperator(3, 20), null, 5));
}

protected void buildCPSolver(EtpModel model, ETPOutput writer)
{
	solver = (properties.minimize) ? new TimetableCPMinSolver(writer) : new TimetableCPSolver(writer);
	solver.read(model);
	this.setLimits();
	this.setRestarts();
	this.setHeuristic();
}


}
