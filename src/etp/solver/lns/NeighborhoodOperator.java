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
/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 13/01/11 - 14:19
 */

import choco.kernel.solver.Solution;

/**
 * NeighborhoodOperator defines how to restrict the search space of a problem around a given solution
 * @author Sophie Demassey
 * @see LNSCPSolver
 */
public interface NeighborhoodOperator {

/**
 * add restrictions (constraints or variable fixing) to the solver associated to the solution
 * @param solution the solution to build the neighborhood around
 * @return true iff the search space is actually shrunken
 */
public boolean restrictNeighborhood(Solution solution);

}
