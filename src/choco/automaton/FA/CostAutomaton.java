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

package choco.automaton.FA;

import choco.automaton.FA.utils.CounterLayerSymbolState;
import choco.automaton.FA.utils.CounterStateLayerSymbol;
import choco.automaton.FA.utils.ICounter;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.variables.integer.IntDomainVar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: julien
 * Date: Nov 23, 2010
 * Time: 11:07:36 AM
 */
public class CostAutomaton extends FiniteAutomaton implements ICostAutomaton {

protected List<ICounter> counters;

public CostAutomaton()
{
	super();
	this.counters = new ArrayList<ICounter>();
}

public CostAutomaton(String regexp)
{
	super(regexp);
	this.counters = new ArrayList<ICounter>();
}

public CostAutomaton(CostAutomaton auto)
{
	super(auto);
	this.counters = new ArrayList<ICounter>();
	this.counters.addAll(auto.getCounters());
}

public CostAutomaton(IAutomaton auto, List<ICounter> counters)
{
	super((FiniteAutomaton) auto);
	this.counters = new ArrayList<ICounter>();
	this.counters.addAll(counters);
}

public CostAutomaton(IAutomaton auto, ICounter counter)
{
	super((FiniteAutomaton) auto);
	this.counters = new ArrayList<ICounter>();
	this.counters.add(counter);
}

public double getWeight(int layer, int symbol)
{
	ICounter zero = counters.get(0);
	return zero.weight(layer, symbol);
}

public double getWeightAtState(int layer, int symbol, int state)
{
	ICounter zero = counters.get(0);
	return zero.weight(layer, symbol, state);
}

public double getWeightAtDimension(int layer, int symbol, int counter)
{
	ICounter aCounter = counters.get(counter);
	return aCounter.weight(layer, symbol);
}

public double getWeightAtDimensionAndState(int layer, int symbol, int counter, int state)
{
	ICounter aCounter = counters.get(counter);
	return aCounter.weight(layer, symbol, state);
}

public int getNbDimensions()
{
	return counters.size();
}

public int getNbLayers()
{
	return (counters == null || counters.isEmpty()) ? 0 : counters.get(0).getNbLayers();
}

@Override
public List<ICounter> getCounters()
{
	return counters;
}

private static int[][] getBounds(IntDomainVar[] cr)
{
	int[][] bounds = new int[2][cr.length];
	for (int i = 0; i < cr.length; i++) {
		bounds[0][i] = cr[i].getInf();
		bounds[1][i] = cr[i].getSup();
	}
	return bounds;
}

private static int[][] getBounds(IntegerVariable[] cr)
{
	int[][] bounds = new int[2][cr.length];
	for (int i = 0; i < cr.length; i++) {
		bounds[0][i] = cr[i].getLowB();
		bounds[1][i] = cr[i].getUppB();
	}
	return bounds;
}

public boolean checkCounter(ICounter c)
{
	assert (this.getNbLayers() == 0 || this.getNbLayers() == c.getNbLayers());
	assert (c.getNbSymbols() >= this.alphabet.size());
	assert (!c.isStateDependent() || c.getNbStates() == this.nbStates);
	return true;
}

public void addCounter(ICounter c)
{
	assert (checkCounter(c));
	this.counters.add(c);
}

public void addPrimaryCounter(ICounter c)
{
	if (counters.isEmpty()) {
		addCounter(c);
	} else {
		assert (checkCounter(c));
		List<ICounter> tmp = new ArrayList<ICounter>(counters.size() + 1);
		tmp.add(c);
		tmp.addAll(counters);
		counters = tmp;
	}
}

public boolean hasStateDependentCounter()
{
	for (ICounter c : counters) {
		if (c.isStateDependent()) {
			return true;
		}
	}
	return false;
}

public boolean hasStateDependentWeightTransition(int symbol, int state)
{
	for (ICounter c : counters) {
		if (c.isStateDependent()) {
			for (int layer = 0; layer < getNbLayers(); layer++) {
				if (c.weight(layer, symbol, state) != 0) {
					return true;
				}
			}
		}
	}
	return false;
}

public static ICostAutomaton makeSingleDimension(IAutomaton pi, int[][][] weightByLayerSymbolState, int min, int max)
{
	ICounter c = new CounterLayerSymbolState(weightByLayerSymbolState, min, max);
	ArrayList<ICounter> tmp = new ArrayList<ICounter>();
	tmp.add(c);
	return (pi == null) ? null : new CostAutomaton(pi, tmp);
}

public static ICostAutomaton makeSingleDimension(IAutomaton pi, int[][] weightByLayerSymbol, int min, int max)
{
	ICounter c = new CounterStateLayerSymbol(weightByLayerSymbol, min, max);
	ArrayList<ICounter> tmp = new ArrayList<ICounter>();
	tmp.add(c);
	return (pi == null) ? null : new CostAutomaton(pi, tmp);
}

public static ICostAutomaton makeMultiDimension(IAutomaton pi, int[][][] weightByLayerSymbolDimension, int[] minByDimension, int[] maxByDimension)
{
	int[][][] ordered = new int[minByDimension.length][weightByLayerSymbolDimension.length][];
	ArrayList<ICounter> tmp = new ArrayList<ICounter>();
	for (int k = 0; k < minByDimension.length; k++) {
		for (int i = 0; i < weightByLayerSymbolDimension.length; i++) {
			ordered[k][i] = new int[weightByLayerSymbolDimension[i].length];
			for (int j = 0; j < weightByLayerSymbolDimension[i].length; j++) {
				ordered[k][i][j] = weightByLayerSymbolDimension[i][j][k];
			}
		}
		tmp.add(new CounterStateLayerSymbol(ordered[k], minByDimension[k], maxByDimension[k]));
	}
	return (pi == null) ? null : new CostAutomaton(pi, tmp);
}

public static ICostAutomaton makeMultiDimension(IAutomaton pi, int[][][][] weightByLayerSymbolDimensionState, int[] minByDimension, int[] maxByDimension)
{
	int[][][][] ordered = new int[minByDimension.length][weightByLayerSymbolDimensionState.length][][];
	ArrayList<ICounter> tmp = new ArrayList<ICounter>();
	for (int k = 0; k < minByDimension.length; k++) {
		//boolean stateDependant = true;
		for (int i = 0; i < weightByLayerSymbolDimensionState.length; i++) {
			ordered[k][i] = new int[weightByLayerSymbolDimensionState[i].length][];
			for (int j = 0; j < weightByLayerSymbolDimensionState[i].length; j++) {
				ordered[k][i][j] = new int[weightByLayerSymbolDimensionState[i][j][k].length];
				for (int q = 0; q < weightByLayerSymbolDimensionState[i][j][k].length; q++) {
					ordered[k][i][j][q] = weightByLayerSymbolDimensionState[i][j][k][q];
				}
				//if (ordered[k][i][j].length == 1)   stateDependant = false;
			}
		}

		//if (stateDependant)
		tmp.add(new CounterLayerSymbolState(ordered[k], minByDimension[k], maxByDimension[k]));
		//else
		//	tmp.add(new CounterStateLayerSymbol(ordered[k],minByDimension[k],maxByDimension[k]));
	}
	return (pi == null) ? null : new CostAutomaton(pi, tmp);
}

public static ICostAutomaton makeMultiDimension(IAutomaton auto, int[][][][] c, IntegerVariable[] z)
{
	int[][] bounds = getBounds(z);
	return makeMultiDimension(auto, c, bounds[0], bounds[1]);
}

public static ICostAutomaton makeMultiDimension(IAutomaton auto, int[][][][] c, IntDomainVar[] z)
{
	int[][] bounds = getBounds(z);
	return makeMultiDimension(auto, c, bounds[0], bounds[1]);
}

public static ICostAutomaton makeMultiDimension(IAutomaton auto, int[][][] c, IntegerVariable[] z)
{
	int[][] bounds = getBounds(z);
	return makeMultiDimension(auto, c, bounds[0], bounds[1]);
}

public static ICostAutomaton makeMultiDimension(IAutomaton auto, int[][][] c, IntDomainVar[] z)
{
	int[][] bounds = getBounds(z);
	return makeMultiDimension(auto, c, bounds[0], bounds[1]);
}


}
