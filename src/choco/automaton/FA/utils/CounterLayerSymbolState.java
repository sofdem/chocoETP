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

package choco.automaton.FA.utils;

import choco.automaton.bounds.SoftBounds;
import choco.automaton.bounds.SoftBoundsFactory;

/**
 * Created by IntelliJ IDEA.
 * User: julien
 * Date: Nov 24, 2010
 * Time: 10:37:53 AM
 */
public class CounterLayerSymbolState implements ICounter, Cloneable {
protected int[][][] weights;
protected SoftBounds bounds;

private CounterLayerSymbolState(SoftBounds bounds)
{
	this.bounds = bounds;
}

private CounterLayerSymbolState(int min, int max)
{
	this(SoftBoundsFactory.makeHardBounds(min, max));
}

public CounterLayerSymbolState(int[][][] weightByLayerSymbolState, int min, int max)
{
	this(min, max);
	this.weights = weightByLayerSymbolState;
}

public CounterLayerSymbolState(int[][][] weightByLayerSymbolState, SoftBounds bounds)
{
	this(bounds);
	this.weights = weightByLayerSymbolState;
}

public void addWeights(ICounter c)
{
	assert c.getNbLayers() == getNbLayers() && c.getNbSymbols() == getNbSymbols() && (!c.isStateDependent() || c.getNbStates() == getNbStates()) : c.getNbLayers() + "==" + getNbLayers() + " && " + c.getNbSymbols() + "==" + getNbSymbols() + " && " + c.getNbStates() + "==" + getNbStates();
	for (int l = 0; l < weights.length; l++) {
		for (int a = 0; a < weights[l].length; a++) {
			for (int q = 0; q < weights[l][a].length; q++) {
				weights[l][a][q] += c.weight(l, a, q);
			}
		}
	}
}

@Override
public SoftBounds bounds()
{
	return bounds;
}

@Override
public int weight(int layer, int symbol)
{
	return weight(layer, symbol, 0);
}

@Override
public int weight(int layer, int symbol, int state)
{
	return this.weights[layer][symbol][state];
}

@Override
public int getNbLayers() { return weights.length; }

@Override
public int getNbSymbols() { return weights[0].length; }

@Override
public int getNbStates() { return weights[0][0].length; }

@Override
public void setWeights(int[][][] weights) { this.weights = weights; }

public void copyLayerWeights(int labelTo, int stateTo, ICounter cFrom, int labelFrom, int stateFrom)
{
	assert this.getNbLayers() == cFrom.getNbLayers();
	for (int l = 0; l < weights.length; l++) {
		this.weights[l][labelTo][stateTo] = cFrom.weight(l, labelFrom, stateFrom);
	}
}

@Override
public boolean isStateDependent() { return true; }

public CounterLayerSymbolState clone()
{
	CounterLayerSymbolState c = new CounterLayerSymbolState(this.bounds());
	if (weights != null) {
		int[][][] w = new int[getNbLayers()][getNbSymbols()][getNbStates()];
		for (int l = 0; l < w.length; l++) {
			for (int a = 0; a < w[l].length; a++) {
				System.arraycopy(this.weights[l][a], 0, w[l][a], 0, w[l][a].length);
			}
		}
		c.setWeights(w);
	}
	return c;
}

}
