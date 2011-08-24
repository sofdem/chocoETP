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
 * Date: 17/02/11 - 10:57
 */

import choco.automaton.FA.ICostAutomaton;
import gnu.trove.TIntObjectHashMap;

/**
 * used for intersecting CostAutomata:
 * a weighted transition in a (deterministic) CostAutomaton is marked by identifying it to a unique dummy symbol
 * @author Julien Menana, Sophie Demassey
 */
public class MarkedTransitions {

/** the next available id */
private char nextId;
/** the list of marked transitions indexed by their ids */
private TIntObjectHashMap<MarkedTransition> transitions;

public MarkedTransitions()
{
	transitions = new TIntObjectHashMap<MarkedTransition>();
	nextId = Character.MAX_VALUE - 100;
}

/**
 * get a marked transition
 * @param id the id of the transition
 * @return the marked transition if it exists or null otherwise
 */
public MarkedTransition getMarkedTransition(int id)
{ return transitions.get(id); }

/**
 * check whether two marked transitions share a common label
 * @param id1 the id of the first transition
 * @param id2 the id of the second transition
 * @return true iff the wo transitions have the same label
 */
public boolean haveCommonLabel(int id1, int id2)
{
	MarkedTransition t1 = this.getMarkedTransition(id1);
	int label1 = (t1 == null) ? id1 : t1.label();
	MarkedTransition t2 = this.getMarkedTransition(id2);
	int label2 = (t2 == null) ? id2 : t2.label();
	return label1 == label2;
}

/**
 * mark a transition by identifying it to a new unique dummy symbol
 * @param automaton the automaton's transition
 * @param label     the label of the transition
 * @param state     the origin state of the transition
 * @return the id of the new marked transition
 */
public int markTransition(ICostAutomaton automaton, int label, int state)
{
	int id = nextId--;
	MarkedTransition transition = new MarkedTransition(id, automaton, label, state);
	transitions.put(id, transition);
	return id;
}

/** One marked transition. */
class MarkedTransition {
	private int id;
	private ICostAutomaton automaton;
	private int label;
	private int state;

	/**
	 * create a marked transition
	 * @param id        the id of the transition
	 * @param automaton the automaton's transition
	 * @param label     the label of the transition
	 * @param state     the origin state of the transition
	 */
	private MarkedTransition(int id, ICostAutomaton automaton, int label, int state)
	{
		this.id = id;
		this.automaton = automaton;
		this.label = label;
		this.state = state;
	}

	public final int id() {return id;}

	public final int label() {return label;}

	public final int state() {return state;}

	public final ICostAutomaton automaton() {return automaton;}

}

}
