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

package choco.automaton.bounds.penalty;

import choco.kernel.common.util.iterators.DisposableIntIterator;
import choco.kernel.solver.variables.integer.IntDomainVar;

/**
 * Created by IntelliJ IDEA.
 * User: julien
 * Date: Apr 30, 2010
 * Time: 1:57:07 PM
 */
public abstract class AbstractPenaltyFunction implements IPenaltyFunction {
public abstract int penalty(int value);

public abstract int penaltyMax(int v1, int v2);

public double minGHat(double lambda, IntDomainVar var)
{
	DisposableIntIterator valIter = var.getDomain().getIterator();
	double ghat = Double.POSITIVE_INFINITY;
	while (valIter.hasNext()) {
		int val = valIter.next();
		ghat = Math.min(ghat, penalty(val) - lambda * val);
	}
	valIter.dispose();
	return ghat;
}

public double maxGHat(double lambda, IntDomainVar var)
{
	DisposableIntIterator valIter = var.getDomain().getIterator();
	double ghat = Double.NEGATIVE_INFINITY;
	while (valIter.hasNext()) {
		int val = valIter.next();
		ghat = Math.max(ghat, penalty(val) - lambda * val);
	}
	valIter.dispose();
	return ghat;
}

public boolean isNull() { return false; }

public boolean isIdentity() { return false; }

public boolean isLinear() { return false; }

}
