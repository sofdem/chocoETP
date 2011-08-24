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

package choco.automaton.bounds;

import choco.automaton.bounds.penalty.IPenaltyFunction;
import choco.automaton.bounds.penalty.LinearPenaltyFunction;
import choco.automaton.bounds.penalty.NullPenaltyFunction;
import choco.kernel.solver.variables.integer.IntDomainVar;

import java.util.ArrayList;
import java.util.List;

/**
 * defines hard (L/U) and preferential (l/u) min/max bounds for some counting variable 'count' and associates some penalty function 'cost'
 * such that
 * 0<=L<=U, L<=l, u<=U and L<=count<=U
 * cost:[L,U]->N
 * cost(count)=costL(count)+costU(count)
 * costL(count)=0 if count>=l
 * costU(count)=0 if count<=u
 * User: julien
 * Date: Nov 23, 2010
 * Time: 11:12:10 AM
 */
public class SoftBounds {

/**
 * factor for MinBound, MaxBound: defines a hard bound B>=0, a preferential bound b>=0, and a function f:N->N such that f(0)=0
 * the associated penalty function 'costB' is defined by:
 * costB(x)=MAX_INT if x is beyond B, f(|x-b|) if x between b and B, 0 otherwise.
 */
abstract class MinMaxBound {
	int hard;
	int soft;
	IPenaltyFunction penaltyFunction;

	public MinMaxBound(int hard, int soft, IPenaltyFunction penaltyFunction)
	{
		this.hard = hard;
		this.soft = soft;
		this.penaltyFunction = (penaltyFunction == null || hard == soft || penaltyFunction.isNull()) ? new NullPenaltyFunction() : penaltyFunction;
//		assert penaltyFunction.penalty(0)==0;
	}

	/**
	 * get the penalty value associated to a possible violation of the soft bound
	 * @param val the value to penalize
	 * @return MAX_INT if val is beyond hard, penaltyFunction(|val-soft|) if x between soft and hard, 0 otherwise.
	 */
	public abstract int penalty(int val);

	/**
	 * get the maximum penalty value reached between bound and hard
	 * @param bound the bound comprises between soft and hard from which to evaluate
	 * @return max penalty(val) for val between bound and hard included
	 */
	public abstract int maxPenaltyFrom(int bound);

	/**
	 * get the maximum penalty value reached between the soft and hard bounds
	 * @return max penalty(val) for val between soft and hard included
	 */
	public int maxPenalty()
	{ return maxPenaltyFrom(soft); }

	/**
	 * shrink the hard bound to the tightest value so as penalty(hard)<=maxPenaltyValue
	 * hard cannot be shrunk beyond to soft, and in case hard=soft, penaltyFunction is set to nullPenaltyFunction
	 * note that after shrinking, maxPenalty()<=maxPenaltyValue if penaltyFunction is non-decreasing
	 * @param maxPenaltyValue >= 0 the maximum penalty value authorized at hard
	 */
	void shrinkHardBound(int maxPenaltyValue)
	{
		assert maxPenaltyValue >= 0;
		int max = maxPenalty();
		if (max == 0 && !(penaltyFunction instanceof NullPenaltyFunction)) {
			penaltyFunction = new NullPenaltyFunction();
		} else if (max > maxPenaltyValue) {
			this.shrink(maxPenaltyValue);
			if (hard == soft) {
				penaltyFunction = new NullPenaltyFunction();
			}
		}
	}

	abstract void shrink(int maxPenaltyValue);

	/** @return true iff the bound is hard (no penalty) */
	boolean isHard()
	{ return penaltyFunction.isNull(); }

}

class MinBound extends MinMaxBound {
	public MinBound(int hard, int soft, IPenaltyFunction penalty)
	{
		super(hard, soft, penalty);
		assert hard <= soft;
	}

	@Override
	public int penalty(int val)
	{
		assert val >= 0;
		return (val < hard) ? Integer.MAX_VALUE : (val < soft) ? penaltyFunction.penalty(soft - val) : 0;
	}

	@Override
	public int maxPenaltyFrom(int bound)
	{
		assert hard <= bound;
		return penaltyFunction.penaltyMax(soft - bound, soft - hard);
	}

	@Override
	void shrink(int maxPenaltyValue)
	{
		int val = hard;
		while (penalty(val) > maxPenaltyValue) {
			hard = ++val;
		}
	}

}

class MaxBound extends MinMaxBound {
	public MaxBound(int hard, int soft, IPenaltyFunction penalty)
	{
		super(hard, soft, penalty);
		assert hard >= soft;
	}

	@Override
	public int penalty(int val)
	{
		assert val >= 0;
		return (val > hard) ? Integer.MAX_VALUE : (val > soft) ? penaltyFunction.penalty(val - soft) : 0;
	}

	@Override
	public int maxPenaltyFrom(int bound)
	{
		assert bound <= hard && bound >= soft;
		return penaltyFunction.penaltyMax(bound - soft, hard - soft);
	}

	@Override
	void shrink(int maxPenaltyValue)
	{
		int val = hard;
		while (penalty(val) > maxPenaltyValue) {
			hard = --val;
		}
	}

	/** @return true iff penalty(x)=x for 0 <= x <= hard */
	boolean isIdentity()
	{ return soft == 0 && penaltyFunction.isIdentity(); }

	/** @return true iff penalty(x)=ax+b for 0 <= x <= hard */
	boolean isLinear()
	{ return soft == 0 && penaltyFunction.isLinear(); }

}

final MinBound min;
final MaxBound max;

protected SoftBounds(int minHard, int minSoft, IPenaltyFunction minPenaltyFunction, int maxHard, int maxSoft, IPenaltyFunction maxPenaltyFunction)
{
	if (minHard > maxHard) {
		throw new RuntimeException("empty interval");
	}
	min = new MinBound(minHard, minSoft, minPenaltyFunction);
	max = new MaxBound(maxHard, maxSoft, maxPenaltyFunction);
}

/**
 * shrink the hard bounds so as penalty(hard)<=maxPenaltyValue
 * @param maxPenaltyValue the maximum authorized penalty value
 * @return true iff min.hard does not exceed max.hard
 */
boolean shrinkHardBounds(int maxPenaltyValue)
{
	if (maxPenaltyValue < 0) {
		throw new RuntimeException("max penalty value must be positive");
	}
	min.shrinkHardBound(maxPenaltyValue);
	max.shrinkHardBound(maxPenaltyValue);
	return getMin() <= getMax();
}

public int getMin() { return min.hard; }

public int getMax() { return max.hard; }

public int getSoftMin() { return min.soft; }

public int getSoftMax() { return max.soft; }

/**
 * get the penalty value associated to a possible violation of the soft bounds
 * @param val the value to penalize
 * @return the penalty value associated to a possible violation of the soft bounds
 */
public int penalty(int val)
{
	return (val < min.hard || val > max.hard) ? Integer.MAX_VALUE : min.penalty(val) + max.penalty(val);
}

/** @return the maximum penalty value reached between min.hard and max.hard */
public int maxPenalty()
{
	if (min.soft <= max.soft) {
		return Math.max(min.maxPenalty(), max.maxPenalty());
	}
	int m = Math.max(min.maxPenaltyFrom(max.soft), max.maxPenaltyFrom(min.soft));
	for (int v = max.soft + 1; v <= min.soft; v++) {
		int p = min.penalty(v) + max.penalty(v);
		if (m < p) { m = p; }
	}
	return m;
}

/** @return true iff penalty(x)=x for 0 <= x <= max.hard */
public boolean isIdentity()
{ return min.isHard() && max.isIdentity(); }

/** @return true iff penalty(x)=ax+b for 0 <= x <= max.hard */
public boolean isLinear()
{ return min.isHard() && max.isLinear(); }

/**
 * create the complementary bounds (n-max)/(n-maxSoft), (n-min)/(n-minSoft)
 * @param n >= 0
 * @return the complementary bounds
 */
public SoftBounds inverseMinMax(int n)
{
	assert n > max.hard;
	return new SoftBounds(n - max.hard, n - max.soft, max.penaltyFunction, n - min.hard, n - min.soft, min.penaltyFunction);
}

/**
 * create the list of pairs relating the values to the associated penalties
 * @return the list (val, penalty(val)) for val between min.hard to max.hard
 */
public List<int[]> makeSoftRelationTable()
{
	List<int[]> hardPenaltyPairs = new ArrayList<int[]>(max.hard - min.hard);
	for (int v = min.hard; v <= max.hard; v++) {
		hardPenaltyPairs.add(new int[]{v, penalty(v)});
	}
	return hardPenaltyPairs;
}

/** @return the factor if the bound is linear, assert error otherwise */
public int getFactor()
{
	assert isLinear();
	return ((LinearPenaltyFunction) max.penaltyFunction).getFactor();
}

/** @return the constant if the bound is linear, assert error otherwise */
public int getConstant()
{
	assert isLinear();
	return ((LinearPenaltyFunction) max.penaltyFunction).getConstant();
}

public double minPenaltyDelta(double factor, IntDomainVar intDomainVar)
{
	throw new RuntimeException("method not implemented");
}

public double maxPenaltyDelta(double factor, IntDomainVar intDomainVar)
{
	throw new RuntimeException("method not implemented");
}

}
