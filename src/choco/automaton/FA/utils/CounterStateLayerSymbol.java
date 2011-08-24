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
 * Time: 10:24:53 AM
 */
public class CounterStateLayerSymbol implements ICounter, Cloneable {

protected int[][][] weights;
protected SoftBounds bounds;

private CounterStateLayerSymbol(SoftBounds bounds)
{
	this.bounds = bounds;
}

private CounterStateLayerSymbol(int min, int max)
{
	this(SoftBoundsFactory.makeHardBounds(min, max));
}

public CounterStateLayerSymbol(int[][] weightByLayerSymbol, SoftBounds bounds)
{
	this(bounds);
	this.weights = new int[1][][];
	this.weights[0] = weightByLayerSymbol;
}

public CounterStateLayerSymbol(int[][] weightByLayerSymbol, int min, int max)
{
	this(weightByLayerSymbol, SoftBoundsFactory.makeHardBounds(min, max));
}

public CounterStateLayerSymbol(int[][][] weightByLayerSymbolState, int min, int max)
{
	this(min, max);
	this.weights = new int[weightByLayerSymbolState[0][0].length][weightByLayerSymbolState.length][weightByLayerSymbolState[0].length];
	for (int q = 0; q < weights.length; q++) {
		for (int l = 0; l < this.weights[q].length; l++) {
			for (int a = 0; a < this.weights[q][l].length; a++) {
				this.weights[q][l][a] = weightByLayerSymbolState[l][a][q];
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
	return this.weights[0][layer][symbol];
}

@Override
public int weight(int layer, int symbol, int state)
{
	return (isStateDependent()) ? weights[state][layer][symbol] : weight(layer, symbol);
}

@Override
public int getNbLayers() { return weights[0].length; }

@Override
public int getNbSymbols() { return weights[0][0].length; }

@Override
public int getNbStates() { return weights.length; }

@Override
public boolean isStateDependent() { return getNbStates() > 1; }

@Override
public void setWeights(int[][][] weightByLayerSymbolState) { throw new RuntimeException("not implemented"); }

public CounterStateLayerSymbol clone()
{
	CounterStateLayerSymbol c = new CounterStateLayerSymbol(this.bounds());
	if (weights != null) {
		int[][][] w = new int[getNbStates()][getNbLayers()][getNbSymbols()];
		for (int q = 0; q < w.length; q++) {
			for (int l = 0; l < w[q].length; l++) {
				System.arraycopy(this.weights[q][l], 0, w[q][l], 0, w[q][l].length);
			}
		}
		c.setWeights(w);
	}
	return c;
}

}
