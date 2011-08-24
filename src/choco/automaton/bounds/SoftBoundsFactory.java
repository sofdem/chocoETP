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
import choco.automaton.bounds.penalty.IdentityPenaltyFunction;
import choco.automaton.bounds.penalty.LinearPenaltyFunction;

/** @author Sophie Demassey */
public class SoftBoundsFactory {


public static SoftBounds makeRelationBounds(int minHard, int maxHard, LinearPenaltyFunction maxPenaltyFunction)
{
	return new SoftBounds(minHard, minHard, null, maxHard, 0, maxPenaltyFunction);
}

public static SoftBounds makeTupleBounds(int minHard, int minSoft, IPenaltyFunction minPenaltyFunction, int maxHard, int maxSoft, IPenaltyFunction maxPenaltyFunction)
{
	return new SoftBounds(minHard, minSoft, minPenaltyFunction, maxHard, maxSoft, maxPenaltyFunction);
}

public static SoftBounds makeShrinkedBounds(int minHard, int minSoft, IPenaltyFunction minPenaltyFunction, int maxHard, int maxSoft, IPenaltyFunction maxPenaltyFunction, int hardSoftLimit)
{
	SoftBounds bounds = makeTupleBounds(minHard, minSoft, minPenaltyFunction, maxHard, maxSoft, maxPenaltyFunction);
	return (bounds.shrinkHardBounds(hardSoftLimit)) ? bounds : null;
}

// f(x)=0 for all min <= x <= max
public static SoftBounds makeHardBounds(int min, int max)
{
	return new SoftBounds(min, min, null, max, max, null);
}

// f(x)=x for all min <= x <= max
public static SoftBounds makeIdentityPenaltyBounds(int min, int max)
{
	return makeRelationBounds(min, max, new IdentityPenaltyFunction());
}

// f(x)=x for all 0 <= x <= max
public static SoftBounds makeIdentityPenaltyBounds(int max)
{
	return makeIdentityPenaltyBounds(0, max);
}

// f(x)=ax+b for all min <= x <= max
public static SoftBounds makeLinearPenaltyBounds(int min, int max, int factor, int offset)
{
	return makeRelationBounds(min, max, new LinearPenaltyFunction(factor, offset));
}

// f(x)=ax for all 0 <= x <= max
public static SoftBounds makeLinearPenaltyBounds(int max, int factor)
{
	return makeLinearPenaltyBounds(0, max, factor, 0);
}


// f(x)=fmin(x)+fmax(x) with
// fmin(x)=a(minPref-x)+b if x in [minHard, minPref], 0 otherwise
// fmax(x)=c(x-maxPref)+d if x in [maxPref, maxHard], 0 otherwise
public static SoftBounds makeMinLinearMaxLinearBounds(int minHard, int minPref, int minFactor, int minOffset, int maxHard, int maxPref, int maxFactor, int maxOffset)
{
	return makeTupleBounds(minHard, minPref, new LinearPenaltyFunction(minFactor, minOffset), maxHard, maxPref, new LinearPenaltyFunction(maxFactor, maxOffset));
}

// f(x)=fmin(x)+fmax(x) with
// fmin(x)=a(minPref-x) if x in [minHard, minPref], 0 otherwise
// fmax(x)=c(x-maxPref) if x in [maxPref, maxHard], 0 otherwise
public static SoftBounds makeMinLinearMaxLinearBounds(int minHard, int minPref, int minFactor, int maxHard, int maxPref, int maxFactor)
{
	return makeMinLinearMaxLinearBounds(minHard, minPref, minFactor, 0, maxHard, maxPref, maxFactor, 0);
}

// f(x)=fmin(x)+fmax(x) with
// fmin(x)=a(minPref-x) if x in [minHard, minPref], 0 otherwise
// fmax(x)=a(x-maxPref) if x in [maxPref, maxHard], 0 otherwise
public static SoftBounds makeMinLinearMaxLinearBounds(int minHard, int minPref, int maxHard, int maxPref, int factor)
{
	return makeMinLinearMaxLinearBounds(minHard, minPref, factor, 0, maxHard, maxPref, factor, 0);
}

// f(x)=a(minPref-x) if x in [minHard, minPref], 0 if x in [minPref, maxHard]
public static SoftBounds makeMinLinearMaxHardBounds(int minHard, int minPref, int minFactor, int minConstant, int maxHard)
{
	return makeTupleBounds(minHard, minPref, new LinearPenaltyFunction(minFactor, minConstant), maxHard, maxHard, null);
}

// f(x)=a(x-maxPref) if x in [maxPref, maxHard], 0 if x in [minHard, maxPref]
public static SoftBounds makeMinHardMaxLinearBounds(int minHard, int maxHard, int maxPref, int maxFactor, int maxConstant)
{
	return makeTupleBounds(minHard, minHard, null, maxHard, maxPref, new LinearPenaltyFunction(maxFactor, maxConstant));
}

}

