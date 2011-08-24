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

package etp.data;

import choco.automaton.FA.utils.CounterLayerSymbolState;
import choco.automaton.FA.utils.CounterStateLayerSymbol;
import choco.automaton.FA.utils.ICounter;
import choco.automaton.bounds.SoftBounds;

import java.util.List;

/** @author Sophie Demassey */
public class EtpCounter implements ICounter, Cloneable {

protected ICounter counter;
String name;
int objectiveCoefficient;

protected EtpCounter(ICounter counter, String name, int objectiveCoefficient)
{
	this.counter = counter;
	this.name = name;
	this.objectiveCoefficient = objectiveCoefficient;
}

public EtpCounter(int[][] weights, SoftBounds bounds, String name, int objectiveCoefficient)
{
	this(new CounterStateLayerSymbol(weights, bounds), name, objectiveCoefficient);
}

public EtpCounter(int[][][] weights, SoftBounds bounds, String name, int objectiveCoefficient)
{
	this(new CounterLayerSymbolState(weights, bounds), name, objectiveCoefficient);
}


public EtpCounter clone()
{
	return new EtpCounter(this.counter.clone(), this.getName(), this.getObjectiveCoefficient());
}


@Override
public SoftBounds bounds() { return counter.bounds(); }

@Override
public int weight(int layer, int symbol) { return counter.weight(layer, symbol); }

@Override
public int weight(int layer, int symbol, int state) { return counter.weight(layer, symbol, state); }

public String getName() { return name; }

@Override
public int getNbLayers() { return counter.getNbLayers(); }

@Override
public int getNbSymbols() { return counter.getNbSymbols(); }

@Override
public int getNbStates() { return counter.getNbStates(); }

@Override
public boolean isStateDependent() { return counter.isStateDependent(); }

public void addWeights(ICounter c) { ((CounterLayerSymbolState) counter).addWeights(c);}

@Override
public void setWeights(int[][][] weights) { counter.setWeights(weights); }

public int getMin() { return bounds().getMin(); }

public int getMax() { return bounds().getMax(); }

public int getMaxPenaltyValue() { return bounds().maxPenalty(); }

public boolean isCostEqualToCounter() { return bounds().isIdentity(); }

public boolean isCostLinearInCounter() { return bounds().isLinear(); }


public int getObjectiveCoefficient() { return objectiveCoefficient; }

public void setObjectiveCoefficient(int c) { objectiveCoefficient = c; }

public List<int[]> makePenaltyRelationTable() { return bounds().makeSoftRelationTable(); }


public static EtpCounter makeGlobalShiftCounter(int nbLayers, int nbSymbols, int[] weightedSymbols, int weight, SoftBounds bounds, int objectiveCoefficient)
{
	int[][] weights = makeGlobalWeightTable(nbLayers, nbSymbols, weightedSymbols, weight);
	return new EtpCounter(weights, bounds, makeName(weightedSymbols, nbSymbols, "G", ""), objectiveCoefficient);
}

public static EtpCounter makeContinuousShiftCounter(int nbLayers, int nbSymbols, int layerMin, int size, int[] weightedSymbols, int weight, SoftBounds bounds, int objectiveCoefficient)
{
	int[][] weights = makeContinuousWeightTable(nbLayers, nbSymbols, layerMin, layerMin + size - 1, weightedSymbols, weight);
	return new EtpCounter(weights, bounds, makeName(weightedSymbols, nbSymbols, "TC", "_" + layerMin + "-" + size), objectiveCoefficient);
}

public static EtpCounter makePeriodicShiftCounter(int nbLayers, int nbSymbols, int layer, int period, int[] weightedSymbols, int weight, SoftBounds bounds, int objectiveCoefficient)
{
	int[][] weights = makePeriodicWeightTable(nbLayers, nbSymbols, layer, period, weightedSymbols, weight);
	return new EtpCounter(weights, bounds, makeName(weightedSymbols, nbSymbols, "TP", "_" + layer + "/" + period), objectiveCoefficient);
}


public boolean isGlobalShiftCounter()
{
	return name.equals("GS");
}

public static String makeName(int[] wSymbols, int nbSymbols, String prefix, String suffix)
{
	return prefix + ((wSymbols.length == nbSymbols - 1 && wSymbols[wSymbols.length - 1] == nbSymbols - 2) ? "S" : (wSymbols.length == 1) ? "_" + wSymbols[0] : "O") + suffix;
}

public static int[][] makeWeightTable(int nbLayers, int nbSymbols, int[] layers, int[] symbols, int weight)
{
	int[][] weights = new int[nbLayers][nbSymbols];
	for (int l : layers) {
		for (int a : symbols) {
			weights[l][a] = weight;
		}
	}
	return weights;
}

protected static int[][] makeWeightTable(int nbLayers, int nbSymbols, int layerMin, int layerMax, int period, int[] symbols, int weight)
{
	assert layerMin <= layerMax;
	int[][] weights = new int[nbLayers][nbSymbols];
	for (int l = layerMin; l <= layerMax; l += period) {
		for (int a : symbols) {
			weights[l][a] = weight;
		}
	}
	return weights;
}

public static int[][] makeContinuousWeightTable(int nbLayers, int nbSymbols, int layerMin, int layerMax, int[] symbols, int weight)
{ return makeWeightTable(nbLayers, nbSymbols, layerMin, layerMax, 1, symbols, weight); }

public static int[][] makePeriodicWeightTable(int nbLayers, int nbSymbols, int layer, int period, int[] symbols, int weight)
{ return makeWeightTable(nbLayers, nbSymbols, layer, nbLayers - 1, period, symbols, weight); }

public static int[][] makeGlobalWeightTable(int nbLayers, int nbSymbols, int[] symbols, int weight)
{ return makeContinuousWeightTable(nbLayers, nbSymbols, 0, nbLayers - 1, symbols, weight); }

}
