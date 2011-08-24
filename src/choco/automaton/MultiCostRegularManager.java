/**
 *  Copyright (c) 1999-2010, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package choco.automaton;

import choco.automaton.FA.IAutomaton;
import choco.automaton.FA.ICostAutomaton;
import choco.cp.model.managers.IntConstraintManager;
import choco.cp.solver.CPSolver;
import choco.kernel.model.ModelException;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.Solver;
import choco.kernel.solver.constraints.SConstraint;
import choco.kernel.solver.variables.integer.IntDomainVar;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: julien
 * Mail: julien.menana{at}emn.fr
 * Date: Mar 23, 2009
 * Time: 4:43:36 PM
 */
public final class MultiCostRegularManager extends IntConstraintManager {

public SConstraint makeConstraint(Solver solver, IntegerVariable[] variables, Object parameters, List<String> options)
{
	if (solver instanceof CPSolver && parameters instanceof Object[]) {

		IntDomainVar[] all = solver.getVar((IntegerVariable[]) variables);

		Object[] param = (Object[]) parameters;
		if (param.length == 3 && param[2] instanceof Object[][][]) {
			int nVars = (Integer) param[0]; // the first nVars variables in 'all' must be the sequence variables
			int nCounters = all.length - nVars;  // the last nCounters variables in 'all' must be the counter variables
			IAutomaton pi = (IAutomaton) param[1];
			Object[][][] costs = (Object[][][]) param[2];

			// try to check whether vars and costVars has been inverted when posting the constraint
			boolean inverted = (nVars != nCounters) ? costs.length == nCounters : !all[0].hasEnumeratedDomain() && all[nVars].hasEnumeratedDomain();
			if (nVars == nCounters && !(all[0].hasEnumeratedDomain() && !all[nVars].hasEnumeratedDomain())) {
				for (int i = 0; !inverted && i < nVars; i++) {
					if (all[i].getSup() > costs[i].length) {
						inverted = true;
					}
				}
			}
			IntDomainVar[] vs;
			IntDomainVar[] z;
			if (!inverted) {
				vs = new IntDomainVar[nVars];
				z = new IntDomainVar[nCounters];
				System.arraycopy(all, 0, vs, 0, nVars);
				System.arraycopy(all, nVars, z, 0, nCounters);
			} else {
				nCounters = nVars;
				nVars = costs.length;
				vs = new IntDomainVar[nVars];
				z = new IntDomainVar[nCounters];
				System.arraycopy(all, 0, z, 0, nCounters);
				System.arraycopy(all, nCounters, vs, 0, nVars);
			}

			// check arguments
			if (vs.length != costs.length && z.length != costs[0][0].length) {
				throw new ModelException("length of arrays are invalid");
			}


			if (param[2] instanceof int[][][]) {
				int[][][] csts = (int[][][]) param[2];
				return new MultiCostRegular(vs, z, pi, csts, solver);
			} else {
				int[][][][] csts = (int[][][][]) param[2];
				return new MultiCostRegular(vs, z, pi, csts, solver);
			}
		} else if (param.length == 2) {
			int nVars = (Integer) param[0]; // the first nVars variables in 'all' must be the sequence variables
			int nCounters = all.length - nVars;  // the last nCounters variables in 'all' must be the counter variables
			ICostAutomaton pi = (ICostAutomaton) param[1];

			IntDomainVar[] vs;
			IntDomainVar[] z;

			vs = new IntDomainVar[nVars];
			z = new IntDomainVar[nCounters];
			System.arraycopy(all, 0, vs, 0, nVars);
			System.arraycopy(all, nVars, z, 0, nCounters);

			return new MultiCostRegular(vs, z, pi, solver);


		}
	}
	throw new ModelException("Could not found a constraint manager in " + this.getClass() + " !");
}

}
