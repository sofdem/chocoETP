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
/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 02/03/11 - 11:27
 */

import etp.output.ETPOutput;

/** @author Sophie Demassey */
public class TimetableCPMinSolver extends TimetableCPSolver {

public TimetableCPMinSolver(ETPOutput writer)
{
	super(writer);
}

@Override
public Boolean solve()
{
	if (Boolean.TRUE != super.minimize(objVar, false)) {
		LOGGER.warning("no solution found");
	} else {
		// LOGGER.info("solution found: " + this.solutionToString());
		this.printRuntimeStatistics();
		this.solutionToFile();
	}
	return isFeasible();
}


}
