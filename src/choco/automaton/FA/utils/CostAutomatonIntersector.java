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

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 16/02/11 - 15:14
 */

import choco.automaton.FA.CostAutomaton;
import choco.automaton.FA.FiniteAutomaton;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intersect CostAutomata sharing the same nb of layers, and the same max nb of symbols:
 * transitions are intersected by successive product of pairs of automata:
 * t=(o1xo2,a,d1xd2) is a transition of the resulting automaton iff t1=(o1,a,d1) is a transition of automaton1 and t2=(o2,a,d2) is a transition of automaton2
 * the weight vectors are only juxtaposed [w11,w12,...,w1k_1,w21,w22,...w2k_2,...]
 * @author Julien Menana, Sophie Demassey
 */
public class CostAutomatonIntersector {


private MarkedTransitions markedTransitions;

public static CostAutomaton intersectCostAutomata(CostAutomaton original, List<CostAutomaton> others)
{
	return new CostAutomatonIntersector().getIntersection(original, others);
}

private CostAutomatonIntersector()
{
	this.markedTransitions = new MarkedTransitions();
}

/**
 * intersect a list of CostAutomata together sharing the same nb of layers, and the same max nb of symbols
 * @param original the initial CostAutomaton
 * @param others   the list of CostAutomata to intersect together to original
 * @return the intersecting automata
 */
private CostAutomaton getIntersection(CostAutomaton original, List<CostAutomaton> others)
{
	if (others == null || others.isEmpty()) return original;

	FiniteAutomaton automaton = original;
	List<ICounter> counters = new ArrayList<ICounter>();
	Map<ICounter, CounterLayerSymbolState> counterMap = new HashMap<ICounter, CounterLayerSymbolState>();

	counters.addAll(this.loadCounters(original, counterMap));
	this.markAutomaton(original);

	for (CostAutomaton pattern : others) {
		counters.addAll(this.loadCounters(pattern, counterMap));
		this.markAutomaton(pattern);
		automaton = this.buildNaiveIntersection(automaton, pattern);
	}
	this.updateWeightsOfStateDependentCounters(automaton, counterMap);
	return new CostAutomaton(automaton, counters);
}

/**
 * add the list of counters of automaton to the resulting list of counters
 * the state-independent counters are simply added to the list
 * the state-dependent counters are duplicated as the states will be re-indexed and the weight matrix will then be updated
 * @param automaton  the automaton to intersect
 * @param counterMap the map of the original state-dependent counters to their duplicate
 * @return the list of counters
 */
private List<ICounter> loadCounters(CostAutomaton automaton, Map<ICounter, CounterLayerSymbolState> counterMap)
{
	for (ICounter counter : automaton.getCounters()) {
		assert counter.getNbLayers() == automaton.getNbLayers();
		assert counter.getNbSymbols() == automaton.getNbSymbols();
		if (counter.isStateDependent()) {
			counterMap.put(counter, new CounterLayerSymbolState(null, counter.bounds()));
		}
	}
	return automaton.getCounters();
}

/**
 * mark each transition weighted by at least one state-dependent counter using one unique identifier
 * the marked transition is recorded, and the label of the transition is then replaced by its id
 * @param automaton the weighted automaton to mark
 */
private void markAutomaton(CostAutomaton automaton)
{
	if (!automaton.hasStateDependentCounter()) return;
	List<int[]> transitions = automaton.getTransitions();
	for (int[] t : transitions) {
		int orig = t[0];
		int dest = t[1];
		int symb = t[2];
		if (automaton.hasStateDependentWeightTransition(symb, orig)) {
			automaton.deleteTransition(orig, dest, symb);
			int id = markedTransitions.markTransition(automaton, symb, orig);
			automaton.addTransition(orig, dest, id);
		}
	}
}


/**
 * the intersection automaton has transitions labeled by some fake symbol each corresponding to
 * relabel all transitions of the resulting automaton labeled with a fake symbol by its corresponding real symbol
 */
private void updateWeightsOfStateDependentCounters(FiniteAutomaton result, Map<ICounter, CounterLayerSymbolState> counterMap)
{
	int nbStates = result.getNbStates();

	for (ICounter c : counterMap.keySet()) {
		counterMap.get(c).setWeights(new int[c.getNbLayers()][c.getNbSymbols()][nbStates]);
	}


	for (int orig = 0; orig < nbStates; orig++) {
		List<int[]> transitions = result.getTransitions(orig);
		TIntHashSet existingTransitions = new TIntHashSet();
		for (int[] t : transitions) {
			int symbol = t[2];
			int dest = t[1];
			MarkedTransitions.MarkedTransition tuple = markedTransitions.getMarkedTransition(symbol);
			if (tuple != null) {
				result.deleteTransition(orig, dest, symbol);
				int label = tuple.label();
				int transitionId = label * nbStates + dest;
				if (!existingTransitions.contains(transitionId)) {
					existingTransitions.add(transitionId);
					result.addTransition(orig, dest, label);
				}
				for (ICounter c : tuple.automaton().getCounters()) {
					CounterLayerSymbolState cs = counterMap.get(c);
					if (cs != null) {
						cs.copyLayerWeights(label, orig, c, label, tuple.state());
					}
				}
			}
		}
	}
	for (ICounter c : counterMap.keySet()) {
		c.setWeights(counterMap.get(c).weights);
	}


}

private int getIntersectionState(FiniteAutomaton pattern, int qOrig, int qPatt)
{
	return qOrig * pattern.getNbStates() + qPatt;
}

private FiniteAutomaton buildNaiveIntersection(FiniteAutomaton original, FiniteAutomaton pattern)
{
	FiniteAutomaton res = new FiniteAutomaton();
	for (int s1 = 0; s1 < original.getNbStates(); s1++) {
		for (int s2 = 0; s2 < pattern.getNbStates(); s2++) {
			res.addState();
			if (original.isFinal(s1) && pattern.isFinal(s2)) {
				res.setFinal(res.getNbStates() - 1);
			}
		}
	}
	res.setInitialState(this.getIntersectionState(pattern, original.getInitialState(), pattern.getInitialState()));

	TIntHashSet outStates1 = new TIntHashSet();
	TIntHashSet outSymbols = new TIntHashSet();

	for (int s1 = 0; s1 < original.getNbStates(); s1++) {
		for (int s2 = 0; s2 < pattern.getNbStates(); s2++) {
			this.buildOutArcs(original, pattern, res, s1, s2, outStates1, outSymbols);
		}
	}
	res.minimize();
	return res;
}


private void buildOutArcs(FiniteAutomaton original, FiniteAutomaton pattern, FiniteAutomaton res, int state1, int state2, TIntHashSet outStates1, TIntHashSet outSymbols)
{
	int[] symbols1 = original.getOutSymbolsArray(state1);
	int[] symbols2 = pattern.getOutSymbolsArray(state2);
	int state = this.getIntersectionState(pattern, state1, state2);

	for (int symbol2 : symbols2) {
		int outState2 = -1;
		try {
			outState2 = pattern.delta(state2, symbol2);
		} catch (FiniteAutomaton.NonDeterministicOperationException e) {
			e.printStackTrace();
		}
		assert outState2 >= 0;

		outStates1.clear();
		outSymbols.clear();
		original.delta(state1, symbol2, outStates1);

		for (int symbol1 : symbols1) {
			if (markedTransitions.haveCommonLabel(symbol1, symbol2)) {
				original.delta(state1, symbol1, outStates1);
				outSymbols.add(symbol1);
			}
		}
		for (TIntIterator it0 = outStates1.iterator(); it0.hasNext(); ) {
			int outState = this.getIntersectionState(pattern, it0.next(), outState2);
			res.addTransition(state, outState, symbol2);
			for (TIntIterator it2 = outSymbols.iterator(); it2.hasNext(); ) {
				res.addTransition(state, outState, it2.next());
			}
		}

	}
}


}
