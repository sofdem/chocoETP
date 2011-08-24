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

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}emn.fr
 * Date: Mar 16, 2010 - 2:09:27 PM
 */

package etp.data.nrp10;

import choco.automaton.FA.CostAutomaton;
import choco.automaton.FA.FiniteAutomaton;
import choco.automaton.FA.utils.CostAutomatonIntersector;
import choco.automaton.FA.utils.ICounter;
import choco.automaton.bounds.SoftBounds;
import choco.automaton.bounds.SoftBoundsFactory;
import etp.data.AutomatonBuilder;
import etp.data.EtpCounter;
import etp.data.components.Activities;
import etp.data.components.Periods;
import nrp.competition.*;

import java.util.*;
import java.util.logging.Level;

//import choco.automaton.penalty.AbstractBoundedPenaltyFunction;
//import etp.components.automata.CostAutomaton;
//import etp.components.automata.Counter;
//import etp.components.automata.CounterType;

/** @author Sophie Demassey */
public class NRP10AutomatonBuilder extends AutomatonBuilder {

// todo dissocier la lecture des donnees/patterns/automates individuels -> dans Data et la creation des automates aggreges correspondant a un contrat -> hors Data


private NRP10PatternParser parser;

//private final Map<String, String[]> unwantedPatterns;
private Map<String, FiniteAutomaton> unwantedAutomata;
private Contracts.Contract contract;


/** first week-end day (1=SUNDAY,..., 7=SATURDAY) */
private int firstWEDay;
/** index of the first week-end day */
private int startWE;
/** number of week-ends in the planning period */
private int nbWE;
/** number of days in the week-end */
private int lengthWE;

public void setContract(Contracts.Contract contract)
{
	this.contract = contract;
	LOGGER.finer("PARSE contract " + this.contract.getID() + ":");
	this.parseWeekendDefinition();
}

public NRP10AutomatonBuilder(Periods periods, Activities activities, Patterns patterns)
{
	super(periods, activities);
	this.parser = new NRP10PatternParser(periods, activities);
	this.parsePatterns(patterns);
}

private void parsePatterns(Patterns patterns)
{
	this.unwantedAutomata = new HashMap<String, FiniteAutomaton>();
	for (Patterns.Pattern pattern : patterns.getPatternArray()) {
		boolean isSoft = true; // todo: InstanceAdapter : add attribute 'on' or 'hard' to <pattern> ?
		this.unwantedAutomata.put(pattern.getID(), parser.generateUnwantedPatternAutomaton(pattern, isSoft));
		if (LOGGER.isLoggable(Level.FINER)) {
			LOGGER.finer("generated " + (isSoft ? "soft" : "hard") + " pattern " + pattern.getID());
		}
	}
}


/*********************************************************************************************************
 *
 *       Generate the Hard Aggregated Automaton for a contract
 *
 *********************************************************************************************************/

/**
 * generate the complement of the union of the automata recognizing all the forbidden words
 * @return the hard automata
 */
@Override
protected FiniteAutomaton generateAggregatedHardAutomaton()
{
	FiniteAutomaton main = new FiniteAutomaton();
	List<FiniteAutomaton> hardAutomata = getForbiddenAutomata();
	for (FiniteAutomaton a : hardAutomata) {
		main = main.union(a);
	}
	for (int a = 0; a < activities.getNbActivities(); a++) {
		main.addToAlphabet(a);
	}
	main = main.complement();
	main.minimize();
	return main;
}

/**
 * get the list of automata recognizing only forbidden words
 * @return the list of hard automata
 */
protected List<FiniteAutomaton> getForbiddenAutomata()
{
	return getHardUnwantedPatternAutomata();
}

/**
 * get the list of automata recognizing the words containing unwanted patterns
 * @return the list of hard automata
 */
private List<FiniteAutomaton> getHardUnwantedPatternAutomata()
{
	List<FiniteAutomaton> automata = new ArrayList<FiniteAutomaton>();
	for (String patternId : contract.getUnwantedPatterns().getPatternArray()) {
		FiniteAutomaton pa = this.unwantedAutomata.get(patternId);
		if (!(pa instanceof CostAutomaton)) {
			automata.add(pa);
			LOGGER.finer("forbidden pattern " + patternId);
		}
	}
	return automata;
}


/*********************************************************************************************************
 *
 *       Generate the Soft Aggregated Automaton for a contract
 *
 *********************************************************************************************************/


/**
 * generate the aggregated hard automaton and the soft automata, then aggregate all
 * @return the aggregated automaton
 */
@Override
protected CostAutomaton generateAggregatedCostAutomaton()
//protected CostAutomaton generateAggregatedCostAutomaton(boolean useGlobalAssignmentCounter, boolean useIndividualAssignmentCounter)
{
//	CostAutomaton hardAutomaton = super.generateAggregatedCostAutomaton(useGlobalAssignmentCounter, useIndividualAssignmentCounter);
	LOGGER.finer("aggregate automaton for contract " + contract.getID());
	CostAutomaton hardAutomaton = super.generateAggregatedCostAutomaton();
	List<CostAutomaton> softAutomata = getSoftAutomata();
	for (CostAutomaton sa : softAutomata) {
		for (ICounter ic : sa.getCounters()) {
			EtpCounter c = (EtpCounter) ic;
			LOGGER.finer(c.getName() + " " + c.getNbLayers() + " " + c.getNbSymbols() + " " + c.getNbStates());
		}
	}
	CostAutomaton ca = CostAutomatonIntersector.intersectCostAutomata(hardAutomaton, softAutomata);
	LOGGER.finer("after intersection");
	for (ICounter ic : ca.getCounters()) {
		EtpCounter c = (EtpCounter) ic;
		LOGGER.finer(c.getName() + " " + c.getNbLayers() + " " + c.getNbSymbols() + " " + c.getNbStates());
	}

//	if (useGlobalAssignmentCounter) {
//		LOGGER.finer("aggregate counters");
//		ca.aggregateIsoCounters();
//	}
	//ca.toDotty("sofauto.dot", ca.nbCounters()-1);
	return ca;
}


/**
 * generate the soft automata
 * @return the list of soft automata
 */
protected List<CostAutomaton> getSoftAutomata()
{
	List<CostAutomaton> automata = new ArrayList<CostAutomaton>();
	CostAutomaton a;
	a = this.getConsecutiveWorkingDaysSoftAutomaton();
	if (a != null) automata.add(a);
	a = this.getConsecutiveFreeDaysSoftAutomaton();
	if (a != null) automata.add(a);
	List<CostAutomaton> ca = this.getSoftUnWantedPatternAutomata();
	if (ca != null) automata.addAll(ca);
	a = this.getWESoftAutomaton();
	if (a != null) automata.add(a);
	a = this.getNoNightBeforeFreeWEAutomaton();
	if (a != null) automata.add(a);
	return automata;
}

private List<CostAutomaton> getSoftUnWantedPatternAutomata()
{
	List<CostAutomaton> automata = new ArrayList<CostAutomaton>();
	for (String patternId : contract.getUnwantedPatterns().getPatternArray()) {
		FiniteAutomaton pa = this.unwantedAutomata.get(patternId);
		if (pa instanceof CostAutomaton) {
			CostAutomaton ca = (CostAutomaton) pa;
			List<ICounter> counters = new ArrayList<ICounter>(ca.getCounters().size());
			for (ICounter c : ca.getCounters()) {
				counters.add(c.clone());
			}
			automata.add(new CostAutomaton(ca, counters));
			LOGGER.finer("soft pattern " + patternId + " iso");
		}
	}
	return (automata.isEmpty()) ? null : automata;
}


/**
 * ******************************************************************************************************
 *
 * Generate the Counters
 *
 * *******************************************************************************************************
 */


private Integer parseOn(OnAndWeight ctr)
{
	if (ctr == null || (ctr.isSetOn() && !ctr.getOn())) return null;
	return ctr.getBigIntegerValue().intValue();
}

private Boolean parseOn(WeightOnly ctr)
{
	if (ctr == null) return null;
	assert ctr.isSetWeight();
	if (ctr.getWeight().intValue() == 0) return null;
	assert ctr.getWeight().intValue() > 0;
	return ctr.getBooleanValue();
}

private Integer getWeight(OnAndWeight ctr)
{
	if (ctr == null) return null;
	int weight = ctr.getWeight().intValue();
	assert weight >= 0;
	return weight;
}

private Integer getWeight(WeightOnly ctr)
{
	if (ctr == null) return null;
	int weight = ctr.getWeight().intValue();
	assert weight >= 0;
	return weight;
}

@Override
protected ICounter generateShiftCounter()
{
	Integer minPref = this.parseOn(contract.getMinNumAssignments());
	Integer minPenalty = this.getWeight(contract.getMinNumAssignments());
	assert minPref == null || minPenalty > 0; // TODO set hard otherwise ?
	if (minPref == null || minPref == 0) {
		minPref = 0;
		minPenalty = 0;
	}

	Integer maxPref = this.parseOn(contract.getMaxNumAssignments());
	Integer maxPenalty = this.getWeight(contract.getMaxNumAssignments());
	assert maxPref == null || maxPenalty > 0;
	if (maxPref == null || maxPref == periods.getNbDays()) {
		maxPref = periods.getNbDays();
		maxPenalty = 0;
	}

	if (minPenalty.equals(maxPenalty) && minPenalty.equals(0)) return null;
	//AbstractBoundedPenaltyFunction bounds = new LinearPenaltyFunction(0, minPref, minPenalty, periods.getNbDays(), maxPref, maxPenalty);
	//CounterType type = new CounterType(CounterType.Period.GLOBAL, CounterType.Activity.SHIFTS,
	//        0, periods.getNbDays(), activities.getShiftArray(Activities.SHIFTS));

	int coef;
	SoftBounds bounds;
	if (minPenalty.equals(0)) {
		coef = maxPenalty;
		bounds = SoftBoundsFactory.makeMinHardMaxLinearBounds(0, maxPenalty * periods.getNbDays(), maxPenalty * maxPref, 1, 0);
	} else if (maxPenalty.equals(0)) {
		coef = minPenalty;
		bounds = SoftBoundsFactory.makeMinLinearMaxHardBounds(0, minPenalty * minPref, minPenalty * periods.getNbDays(), 1, 0);
	} else if (minPenalty.equals(maxPenalty)) {
		coef = minPenalty;
		bounds = SoftBoundsFactory.makeMinLinearMaxLinearBounds(0, minPenalty * minPref, minPenalty * periods.getNbDays(), minPenalty * maxPref, 1);
	} else {
		coef = 1;
		bounds = SoftBoundsFactory.makeMinLinearMaxLinearBounds(0, minPref, minPenalty, periods.getNbDays(), maxPref, maxPenalty);
	}
	LOGGER.finer("soft nb working days: [" + minPref + "," + maxPref + "] linear(" + minPenalty + "," + maxPenalty + ")");
	return EtpCounter.makeGlobalShiftCounter(periods.getNbDays(), activities.getNbActivities(), activities.getShiftArray(activities.SHIFTS), coef, bounds, 1);
}

@Override
protected List<ICounter> generateShiftTypeCounters()
{ return null; }

private CostAutomaton getConsecutiveWorkingDaysSoftAutomaton()
{
	// ConsecutiveWorkingDays bounds are assumed to be either soft (on=1, weight=0) or unspecified (on=0)
	Integer minPref = this.parseOn(contract.getMinConsecutiveWorkingDays());
	Integer minPenalty = this.getWeight(contract.getMinConsecutiveWorkingDays());
	Integer maxPref = this.parseOn(contract.getMaxConsecutiveWorkingDays());
	Integer maxPenalty = this.getWeight(contract.getMaxConsecutiveWorkingDays());

	CostAutomaton a = getConsecutiveSoftAutomaton(minPref, minPenalty, maxPref, maxPenalty, activities.getShiftArray(activities.SHIFTS), activities.getShiftArray(activities.REST), "consW");
	if (a != null) {
		LOGGER.finer("soft nb consecutive working days: [" + ((minPref != null) ? minPref : "-") + "," + ((maxPref != null) ? maxPref : "-") + "] iso");
	}
	return a;
}

private CostAutomaton getConsecutiveFreeDaysSoftAutomaton()
{
	// ConsecutiveFreeDays bounds are assumed to be either soft (on=1, weight=0) or unspecified (on=0)
	Integer minPref = this.parseOn(contract.getMinConsecutiveFreeDays());
	Integer minPenalty = this.getWeight(contract.getMinConsecutiveFreeDays());
	Integer maxPref = this.parseOn(contract.getMaxConsecutiveFreeDays());
	Integer maxPenalty = this.getWeight(contract.getMaxConsecutiveFreeDays());

	CostAutomaton a = getConsecutiveSoftAutomaton(minPref, minPenalty, maxPref, maxPenalty, activities.getShiftArray(activities.REST), activities.getShiftArray(activities.SHIFTS), "consR");
	if (a != null) {
		LOGGER.finer("soft nb consecutive free days: [" + ((minPref != null) ? minPref : "-") + "," + ((maxPref != null) ? maxPref : "-") + "] iso");
	}
	return a;
}

private CostAutomaton getConsecutiveSoftAutomaton(Integer minPref, Integer minPenalty, Integer maxPref, Integer maxPenalty, int[] act, int[] notAct, String suffix)
{
	// consecutive bounds are assumed to be either soft (on=1, weight>0) or unspecified (on=0)
	if (minPref == null && maxPref == null) return null;

	int lastState = (maxPref == null) ? minPref : maxPref;
	assert lastState > 0;

	CostAutomaton pattern = new CostAutomaton();
	for (int i = 0; i <= lastState; i++) {
		pattern.addState();
		pattern.setFinal(i);
		pattern.addTransition(i, 0, notAct);
	}
	pattern.setInitialState(0);
	for (int i = 0; i < lastState; i++) { pattern.addTransition(i, i + 1, act); }
	pattern.addTransition(lastState, lastState, act);

	int[][][] weights = new int[periods.getNbDays()][activities.getNbActivities()][lastState + 1];
	if (maxPref != null) {
		assert maxPenalty > 0;
		for (int a : act) { for (int t = 0; t < weights.length; t++) { weights[t][a][lastState] = maxPenalty; } }
	}
	if (minPref != null) {
		assert minPref > 0 && minPref <= lastState;
		assert minPenalty > 0;
		int w = minPenalty;
		for (int i = minPref - 1; i > 0; i--) {
			for (int na : notAct) { for (int t = 0; t < weights.length; t++) { weights[t][na][i] = w; } }
			for (int a : act) { weights[weights.length - 1][a][i - 1] = w; }
			w += minPenalty;
		}
	}
	//CounterType cardType = new CounterType(CounterType.Class.PATTERN, suffix);
	//AbstractBoundedPenaltyFunction cardBound = new IdentityPenaltyFunction(Math.max(minPenalty, maxPenalty)* periods.getNbDays());
	//List<Counter> counters = new ArrayList<Counter>();
	//counters.add(new Counter(cardType, cardBound));
	//pattern.setCounters(counters, costs);
	SoftBounds bounds = SoftBoundsFactory.makeIdentityPenaltyBounds(Math.max(minPenalty, maxPenalty) * periods.getNbDays());
	pattern.addCounter(new EtpCounter(weights, bounds, "C" + suffix, 1));
	return pattern;
}

private CostAutomaton getWESoftAutomaton()
{
	if (this.parseOn(contract.getMaxWorkingWeekendsInFourWeeks()) == null && this.parseOn(contract.getCompleteWeekends()) == null && this.parseOn(contract.getIdenticalShiftTypesDuringWeekend()) == null) {
		return null;
	}
	CostAutomaton pattern = parser.buildWEAutomaton(lengthWE, startWE);
	int lastWEDay = startWE + lengthWE - 1;
	int nbStatesInLastLayer = (lengthWE > 2) ? activities.getNbActivities() + 2 : activities.getNbActivities();
	ICounter c;
	c = this.addNumberWECounter(pattern.getNbStates(), nbStatesInLastLayer, lastWEDay);
	if (c != null) pattern.addCounter(c);
	c = this.addCompleteWECounter(pattern.getNbStates(), nbStatesInLastLayer, lastWEDay);
	if (c != null) pattern.addCounter(c);
	c = this.addIdenticalWECounter(pattern.getNbStates(), nbStatesInLastLayer, lastWEDay);
	if (c != null) pattern.addCounter(c);
	assert !pattern.getCounters().isEmpty();
	return pattern;
}

private ICounter addNumberWECounter(int nbStates, int nbStatesInLastLayer, int lastWEDay)
{
	Integer maxInFour = this.parseOn(contract.getMaxWorkingWeekendsInFourWeeks());
	if (maxInFour == null) return null;

	Integer maxInFourPenalty = this.getWeight(contract.getMaxWorkingWeekendsInFourWeeks());
	if (nbWE != 4 && maxInFour > 4) throw new RuntimeException("weekend definition : not handled");
	//SoftBounds cardBound = new SoftBounds(0,0,0, nbWE, maxInFour, maxInFourPenalty);
	int[][][] weights = new int[periods.getNbDays()][activities.getNbActivities()][nbStates];
	this.replicateWECosts(weights, this.getWorkedWETransitionCosts(true, nbStatesInLastLayer, maxInFourPenalty), lastWEDay);
	// AbstractBoundedPenaltyFunction cardBound = new LinearPenaltyFunction(0,0,0,nbWE, maxInFour, maxInFourPenalty);
	//counters.add(new Counter(new CounterType(CounterType.Class.PATTERN, "#WE"), cardBound));
	LOGGER.finer("soft max nb working WE: " + maxInFour + " iso");
	SoftBounds bounds = SoftBoundsFactory.makeMinHardMaxLinearBounds(0, maxInFourPenalty * nbWE, maxInFourPenalty * maxInFour, 1, 0);
	return new EtpCounter(weights, bounds, "#WE", 1);
}

private ICounter addCompleteWECounter(int nbStates, int nbStatesInLastLayer, int lastWEDay)
{
	Boolean complete = this.parseOn(contract.getCompleteWeekends());
	Integer completePenalty = this.getWeight(contract.getCompleteWeekends());
	if (complete == null) return null;
	//SoftBounds cardBound = new SoftBounds(nbWE*completePenalty) ;
	//AbstractBoundedPenaltyFunction cardBound = new IdentityPenaltyFunction(nbWE*completePenalty);
	//counters.add(new Counter(new CounterType(CounterType.Class.PATTERN, "fullWE"), cardBound));
	int[][][] weights = new int[periods.getNbDays()][activities.getNbActivities()][nbStates];
	this.replicateWECosts(weights, this.getCompleteWETransitionCosts(complete, nbStatesInLastLayer, completePenalty), lastWEDay);
	LOGGER.finer("soft max nb complete WE: " + complete + " iso");
	SoftBounds bounds = SoftBoundsFactory.makeIdentityPenaltyBounds(nbWE * completePenalty);
	return new EtpCounter(weights, bounds, "fullWE", 1);
}

private ICounter addIdenticalWECounter(int nbStates, int nbStatesInLastLayer, int lastWEDay)
{
	Boolean identical = this.parseOn(contract.getIdenticalShiftTypesDuringWeekend());
	Integer identicalPenalty = this.getWeight(contract.getIdenticalShiftTypesDuringWeekend());
	if (identical == null) return null;
	//SoftBounds cardBound = new SoftBounds(nbWE*identicalPenalty) ;
	//AbstractBoundedPenaltyFunction cardBound = new IdentityPenaltyFunction(nbWE*identicalPenalty);
	//counters.add(new Counter(new CounterType(CounterType.Class.PATTERN, "idWE"), cardBound));
	int[][][] weights = new int[periods.getNbDays()][activities.getNbActivities()][nbStates];
	this.replicateWECosts(weights, this.getIdenticalWETransitionCosts(identical, nbStatesInLastLayer, identicalPenalty), lastWEDay);
	LOGGER.finer("soft max nb identical WE: " + identical + " iso");
	SoftBounds bounds = SoftBoundsFactory.makeIdentityPenaltyBounds(nbWE * identicalPenalty);
	return new EtpCounter(weights, bounds, "idWE", 1);
}

private ICounter addConsecutiveWECounter(int nbStates, int nbStatesInLastLayer, int lastWEDay)
{
	Integer minCons = this.parseOn(contract.getMinConsecutiveWorkingWeekends());
	Integer minConsPenalty = this.getWeight(contract.getMinConsecutiveWorkingWeekends());
	Integer maxCons = this.parseOn(contract.getMaxConsecutiveWorkingWeekends());
	Integer maxConsPenalty = this.getWeight(contract.getMaxConsecutiveWorkingWeekends());
	if (minCons != null || maxCons != null) System.err.println("min/maxConsecutiveWE : not implemented");
	return null;
}

private void parseWeekendDefinition()
{
	int lastDay = -1;
	if (!contract.isSetWeekendDefinition()) {
		firstWEDay = Calendar.SATURDAY;
		lastDay = Calendar.SUNDAY;
		lengthWE = 2;
	} else {
		switch (contract.getWeekendDefinition().intValue()) {
		case Weekend.INT_SATURDAY_SUNDAY:
			firstWEDay = Calendar.SATURDAY;
			lastDay = Calendar.SUNDAY;
			lengthWE = 2;
			break;
		case Weekend.INT_SATURDAY_SUNDAY_MONDAY:
			firstWEDay = Calendar.SATURDAY;
			lastDay = Calendar.MONDAY;
			lengthWE = 3;
			break;
		case Weekend.INT_FRIDAY_SATURDAY_SUNDAY:
			firstWEDay = Calendar.FRIDAY;
			lastDay = Calendar.SUNDAY;
			lengthWE = 3;
			break;
		case Weekend.INT_FRIDAY_SATURDAY_SUNDAY_MONDAY:
			firstWEDay = Calendar.FRIDAY;
			lastDay = Calendar.MONDAY;
			lengthWE = 4;
			break;
		}
	}
	this.startWE = periods.getFirstIndexForDayOfWeek(firstWEDay);
	this.nbWE = periods.getNumberOfIndexForDayOfWeek(startWE);
	if (startWE + lengthWE - 1 != periods.getFirstIndexForDayOfWeek(lastDay)) {
		throw new RuntimeException("weekend definition : not handled");
	}
	if (startWE + 7 * (nbWE - 1) + lengthWE > periods.getNbDays()) {
		throw new RuntimeException("weekend definition : not handled");
	}
}


private void replicateWECosts(int[][][] toCosts, int[][] fromCosts, int day)
{
	int offsetState = 1;
	int nbStates = fromCosts[0].length;
	while (day < toCosts.length) {
		for (int a = 0; a < fromCosts.length; a++) {
			System.arraycopy(fromCosts[a], 0, toCosts[day][a], offsetState, nbStates);
		}
		day += 7;
	}
}

private int[][] getWorkedWETransitionCosts(boolean countNotWorked, int nbStates, int weight)
{
	int rest = activities.getRestIndex();
	assert rest == activities.getNbActivities() - 1;
	int[][] costs = new int[activities.getNbActivities()][nbStates];
	if (countNotWorked) {
		costs[rest][rest] = weight;
	} else {
		for (int a = 0; a < costs.length; a++) { for (int q = 0; q < nbStates; q++) { costs[a][q] = weight; } }
		costs[rest][rest] = 0;
	}
	return costs;
}

private int[][] getCompleteWETransitionCosts(boolean countNotComplete, int nbStates, int weight)
{
	int rest = activities.getRestIndex();
	assert rest == activities.getNbActivities() - 1;
	int[][] costs = new int[activities.getNbActivities()][nbStates];
	if (countNotComplete) {
		for (int q = 0; q < nbStates; q++) { costs[rest][q] = weight; }
		costs[rest][rest] = 0;
		for (int a = 0; a < rest; a++) {
			costs[a][rest] = weight;
			if (rest + 2 < nbStates) costs[a][rest + 2] = weight;
		}
	} else {
		for (int a = 0; a < rest; a++) {
			for (int q = 0; q < nbStates; q++) { costs[a][q] = weight; }
			costs[a][rest] = 0;
			if (rest + 2 < nbStates) costs[a][rest + 2] = 0;
		}
		costs[rest][rest] = weight;
	}
	return costs;
}

private int[][] getIdenticalWETransitionCosts(boolean countNotIdentical, int nbStates, int weight)
{
	int rest = activities.getRestIndex();
	assert rest == activities.getNbActivities() - 1;
	int[][] costs = new int[activities.getNbActivities()][nbStates];
	if (countNotIdentical) {
		for (int a = 0; a < costs.length; a++) {
			for (int q = 0; q < nbStates; q++) { if (a != q) costs[a][q] = weight; }
		}
	} else {
		for (int a = 0; a < costs.length; a++) { costs[a][a] = weight; }
	}
	return costs;
}

private CostAutomaton getNoNightBeforeFreeWEAutomaton()
{
	Boolean noNight = this.parseOn(contract.getNoNightShiftBeforeFreeWeekend());
	if (Boolean.TRUE != noNight) {
		return null;
	}
	Integer noNightPenalty = this.getWeight(contract.getNoNightShiftBeforeFreeWeekend());
	CostAutomaton a = parser.buildNoNightBeforeFreeWEAutomaton(lengthWE, firstWEDay, noNightPenalty);
	LOGGER.finer((a != null) ? "posted soft pattern noNightBeforeFreeWE" : "not posted soft pattern noNightShiftBeforeFreeWE cos' no night shift is defined");
	return a;
}


}
