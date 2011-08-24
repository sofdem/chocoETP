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
 * Date: Mar 1, 2010 - 1:47:21 PM
 */

package etp.data.asap3;

import asap3.Contracts;
import asap3.Pattern;
import asap3.PatternMatch;
import choco.automaton.FA.CostAutomaton;
import choco.automaton.FA.FiniteAutomaton;
import choco.automaton.FA.utils.CostAutomatonIntersector;
import choco.automaton.FA.utils.ICounter;
import choco.automaton.bounds.SoftBounds;
import choco.automaton.bounds.SoftBoundsFactory;
import choco.automaton.bounds.penalty.ConstantPenaltyFunction;
import choco.automaton.bounds.penalty.IPenaltyFunction;
import choco.automaton.bounds.penalty.LinearPenaltyFunction;
import choco.automaton.bounds.penalty.QuadraticPenaltyFunction;
import choco.kernel.common.logging.ChocoLogging;
import etp.data.AutomatonBuilder;
import etp.data.EtpCounter;
import etp.data.components.Activities;
import etp.data.components.Periods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/** @author Sophie Demassey */
public class ASAP3AutomatonBuilder extends AutomatonBuilder {

private ASAP3PatternParser parser;
private int hardSoftLimit;
private Map<String, ICounter> globalCounters;
private Map<String, ICounter> periodCounters;
private Map<String, ICounter> weekCounters;

private Map<ConstraintType, List<PatternMatch>> distribution;

public enum ConstraintType {
	HARD_PATTERN,
	SOFT_PATTERN,
	HARD_PATTERN_COUNT,
	SOFT_PATTERN_COUNT,
	GLOBAL_COUNTER,
	CONT_PERIOD_COUNTER,
	NON_CONT_PERIOD_COUNTER,
	OTHER
}


public ASAP3AutomatonBuilder(Periods periods, Activities activities, int hardSoftLimit)
{
	super(periods, activities);
	parser = new ASAP3PatternParser(periods, activities);
	this.hardSoftLimit = hardSoftLimit;
}

public void setContract(Contracts.Contract contract)
{
	if (contract.isSetWorkload()) LOGGER.warning("WARNING not parsed element: Contract/Workloads");
	this.distributePatterns(contract.getPatterns().getMatchArray());
	this.setCounters();
}

private void distributePatterns(PatternMatch[] matches)
{
	this.distribution = new HashMap<ConstraintType, List<PatternMatch>>();
	for (ConstraintType type : ConstraintType.values()) { distribution.put(type, new ArrayList<PatternMatch>()); }
	for (PatternMatch match : matches) { distribution.get(getMatchType(match)).add(match); }
	if (LOGGER.isLoggable(Level.FINER)) {
		for (ConstraintType s : distribution.keySet()) {
			LOGGER.finer(s.name() + ": " + distribution.get(s).size());
		}
	}
}


private ConstraintType getMatchType(PatternMatch match)
{
	if (isSingleShift(match)) {
		return isGlobal(match) && isGlobal(match.getPatternArray(0)) ? ConstraintType.GLOBAL_COUNTER : (!match.getPatternArray(0).isSetStartDay()) ? ConstraintType.CONT_PERIOD_COUNTER : ConstraintType.NON_CONT_PERIOD_COUNTER;
	}

	if (match.isSetMax()) {
		boolean hard = match.getMax().getWeight().getBigIntegerValue().intValue() > hardSoftLimit;
		if (match.getMax().getCount().intValue() == 0) {
			return (hard) ? ConstraintType.HARD_PATTERN : ConstraintType.SOFT_PATTERN;
		}
		return (hard) ? ConstraintType.HARD_PATTERN_COUNT : ConstraintType.SOFT_PATTERN_COUNT;
	}
	return ConstraintType.OTHER;
}

private boolean isGlobal(PatternMatch match)
{ return !(match.isSetRegionEnd() || match.isSetRegionEndDate() || match.isSetRegionStart() || match.isSetRegionStartDate()); }

private boolean isGlobal(Pattern p)
{ return !p.isSetStartDay() && parser.getStartPeriod(p) <= 0; }


private boolean isSingleShift(PatternMatch match)
{
	if (match.sizeOfPatternArray() != 1) return false;
	Pattern p = match.getPatternArray(0);
	return parser.getSize(p) == 1;
}


/**
 * generate the complement of the union of the automata recognizing all the forbidden words
 * parse the "bad pattern" contract rules and merge them into one automaton.
 * A bad pattern is a sub-sequence of activities forbidden in any solution shift.
 * A solution shift must then be recognized by the complement of the union of the bad pattern automata.
 * The complement automaton is built by iteratively adding (union) each bad pattern automaton and minimizing.
 * @return the minimized intersection DFA of the contract rules
 */
@Override
protected FiniteAutomaton generateAggregatedHardAutomaton()
{
	List<PatternMatch> matches = distribution.get(ConstraintType.HARD_PATTERN);
	FiniteAutomaton main = null;
	for (PatternMatch match : matches) {
		int matchStart = 0;
		if (match.isSetRegionStart()) {
			matchStart = match.getRegionStart().intValue();
		} else if (!isGlobal(match)) {
			LOGGER.warning("WARNING ignored: PatternMatch/Region..");
		}
		LOGGER.finer("posted hard pattern ");
		for (Pattern pattern : match.getPatternArray()) {
			FiniteAutomaton automaton = parser.generateUnwantedPatternAutomaton(pattern, matchStart, 0);
			ChocoLogging.flushLogs();
			main = (main == null) ? automaton : main.union(automaton);
			main.minimize();
		}
	}
	if (main == null) main = new FiniteAutomaton();

	for (int a = 0; a < activities.getNbActivities(); a++) {
		main.addToAlphabet(a);
	}
	main = main.complement();
	main.minimize();
	return main;
}
// todo: ordonner les patterns par taille croissante ou merger ensemble les patterns d'un mme match


/**
 * generate the aggregated hard automaton and the soft automata, then aggregate all
 * @return the aggregated automaton
 */
@Override
protected CostAutomaton generateAggregatedCostAutomaton()
//protected CostAutomaton generateAggregatedCostAutomaton(boolean useGlobalAssignmentCounter, boolean useIndividualAssignmentCounter)
{
//	CostAutomaton hardAutomaton = super.generateAggregatedCostAutomaton(useGlobalAssignmentCounter, useIndividualAssignmentCounter);
	LOGGER.finer("aggregate automaton for contract ");
	CostAutomaton hardAutomaton = super.generateAggregatedCostAutomaton();

	List<CostAutomaton> softAutomata = new ArrayList<CostAutomaton>();
	List<PatternMatch> matches = distribution.get(ConstraintType.SOFT_PATTERN);
	for (PatternMatch match : matches) {
		int matchStart = 0;
		if (match.isSetRegionStart()) {
			matchStart = match.getRegionStart().intValue();
		} else if (!isGlobal(match)) {
			LOGGER.warning("WARNING ignored: PatternMatch/Region..");
		}
		int weight = match.getMax().getWeight().getBigIntegerValue().intValue();
		LOGGER.finer("posted soft pattern " + weight);
		for (Pattern pattern : match.getPatternArray()) {
			CostAutomaton sa = (CostAutomaton) parser.generateUnwantedPatternAutomaton(pattern, matchStart, weight);
			if (sa != null) softAutomata.add(sa);
			ChocoLogging.flushLogs();
		}
	}
	if (LOGGER.isLoggable(Level.FINER)) {
		for (CostAutomaton sa : softAutomata) {
			for (ICounter ic : sa.getCounters()) {
				EtpCounter c = (EtpCounter) ic;
				LOGGER.finer(c.getName() + " " + c.getNbLayers() + " " + c.getNbSymbols() + " " + c.getNbStates());
			}
		}
	}
	CostAutomaton ca = CostAutomatonIntersector.intersectCostAutomata(hardAutomaton, softAutomata);
	if (LOGGER.isLoggable(Level.FINER)) {
		LOGGER.finer("after intersection");
		for (ICounter ic : ca.getCounters()) {
			EtpCounter c = (EtpCounter) ic;
			LOGGER.finer(c.getName() + " " + c.getNbLayers() + " " + c.getNbSymbols() + " " + c.getNbStates());
		}
	}
//	if (useGlobalAssignmentCounter) {
//		LOGGER.finer("aggregate counters");
//		ca.aggregateIsoCounters();
//	}
	//ca.toDotty("sofauto" + nono + ".dot");
	//nono++;
	return ca;
}
//int nono=0;

enum MatchPenaltyFunction {
	LINEAR
			{
				IPenaltyFunction getPenaltyFunction(int weight) { return new LinearPenaltyFunction(weight); }
			},
	QUADRATIC
			{
				IPenaltyFunction getPenaltyFunction(int weight) { return new QuadraticPenaltyFunction(weight); }
			},
	CONSTANT
			{
				IPenaltyFunction getPenaltyFunction(int weight) { return new ConstantPenaltyFunction(weight); }
			},
	HARD
			{
				IPenaltyFunction getPenaltyFunction(int weight) { return null; }
			};

	abstract IPenaltyFunction getPenaltyFunction(int weight);
}

private SoftBounds getBounds(PatternMatch match, int lb, int ub)
{
	String label = "";
	int minPref = lb;
	int minPenalty = 0;
	MatchPenaltyFunction minType = MatchPenaltyFunction.HARD;
	if (match.isSetMin()) {
		if (match.getMin().isSetLabel()) label += match.getMin().getLabel();
		minPref = match.getMin().getCount().intValue();
		assert minPref >= lb;
		if (minPref > lb && match.getMin().isSetWeight()) {
			minPenalty = match.getMin().getWeight().getBigIntegerValue().intValue();
			minType = (match.getMin().getWeight().isSetFunction()) ? MatchPenaltyFunction.valueOf(match.getMin().getWeight().getFunction().toUpperCase()) : MatchPenaltyFunction.LINEAR;
		} else {
			lb = minPref;
		}

	}
	int maxPref = ub;
	int maxPenalty = 0;
	MatchPenaltyFunction maxType = MatchPenaltyFunction.HARD;
	if (match.isSetMax()) {
		if (match.getMax().isSetLabel() && !label.equals(match.getMax().getLabel())) label += match.getMax().getLabel();
		maxPref = match.getMax().getCount().intValue();
		assert maxPref <= ub : " prefered max > hard max";
		if (match.getMax().isSetWeight()) {
			maxPenalty = match.getMax().getWeight().getBigIntegerValue().intValue();
			maxType = (match.getMax().getWeight().isSetFunction()) ? MatchPenaltyFunction.valueOf(match.getMax().getWeight().getFunction().toUpperCase()) : MatchPenaltyFunction.LINEAR;
		} else {
			ub = maxPref;
		}
	}
	LOGGER.finer(label + " min " + minType + " max " + maxType);
	return SoftBoundsFactory.makeShrinkedBounds(lb, minPref, minType.getPenaltyFunction(minPenalty), ub, maxPref, maxType.getPenaltyFunction(maxPenalty), hardSoftLimit);
}

private int getStartRegion(PatternMatch match)
{
	return match.isSetRegionStart() ? match.getRegionStart().intValue() : match.isSetRegionStartDate() ? periods.getDayIndex(match.getRegionStartDate()) : 0;
}

private int getSizeRegion(PatternMatch match, int start)
{
	int end = match.isSetRegionEnd() ? match.getRegionEnd().intValue() + 1 : match.isSetRegionEndDate() ? periods.getDayIndex(match.getRegionEndDate()) + 1 : periods.getNbDays();
	return end - start;
}

private void setCounters()
{
	// todo: multiply weight by penalty if unique
	this.globalCounters = new HashMap<String, ICounter>();
	LOGGER.finer("GLOBAL COUNTER");
	for (PatternMatch match : distribution.get(ConstraintType.GLOBAL_COUNTER)) {
		int[] actIdx = parseActivities(match);
		SoftBounds bounds = getBounds(match, 0, periods.getNbDays());
		if (bounds != null) {
			EtpCounter counter = EtpCounter.makeGlobalShiftCounter(periods.getNbDays(), activities.getNbActivities(), actIdx, 1, bounds, 1);
			globalCounters.put(counter.getName(), counter);
		}
	}
	LOGGER.finer("CONTIG COUNTER");
	this.weekCounters = new HashMap<String, ICounter>();
	for (PatternMatch match : distribution.get(ConstraintType.CONT_PERIOD_COUNTER)) {
		int start = this.getStartRegion(match);
		int size = this.getSizeRegion(match, start);
		assert size < periods.getNbDays();
		int[] actIdx = parseActivities(match);
		SoftBounds bounds = getBounds(match, 0, size);
		if (bounds != null) {
			EtpCounter counter = EtpCounter.makeContinuousShiftCounter(periods.getNbDays(), activities.getNbActivities(), start, size, actIdx, 1, bounds, 1);
			weekCounters.put(counter.getName(), counter);
		}
	}
	LOGGER.finer("NON CONTIG COUNTER");
	this.periodCounters = new HashMap<String, ICounter>();
	for (PatternMatch match : distribution.get(ConstraintType.NON_CONT_PERIOD_COUNTER)) {
		assert (this.getStartRegion(match) <= 0 && match.getPatternArray(0).isSetStartDay()) : match;
		int day = periods.getFirstIndexForDayOfWeek(match.getPatternArray(0).getStartDay());
		int size = periods.getNumberOfIndexForDayOfWeek(day);
		int[] actIdx = parseActivities(match);
		SoftBounds bounds = getBounds(match, 0, size);
		if (bounds != null) {
			EtpCounter counter = EtpCounter.makePeriodicShiftCounter(periods.getNbDays(), activities.getNbActivities(), day, 7, actIdx, 1, bounds, 1);
			periodCounters.put(counter.getName(), counter);
		}
	}
	LOGGER.finer(globalCounters.size() + " global counters, " + periodCounters.size() + " period counters");
}


private int[] parseActivities(PatternMatch match)
{
	assert match.sizeOfPatternArray() == 1;
	Pattern p = match.getPatternArray(0);

	if (p.sizeOfShiftArray() == 1) { // any shift, or REST, ALL, SHIFTS
		String groupId = p.getShiftArray(0);
		assert !groupId.equals(activities.ALL) : "global counter of all activities : no sense !";
		return activities.getShiftArray(groupId);
	}
	if (p.sizeOfShiftGroupArray() == 1) { // any subgroup of at least 1 shift
		String groupId = p.getShiftGroupArray(0);
		return activities.getShiftGroup(groupId);
	}
	if (p.sizeOfNotShiftArray() == 1) { // any shift --> NotGroup is a group of activities including REST
		String groupId = p.getNotShiftArray(0);
		return activities.getNotShiftArray(groupId);

	}
	if (p.sizeOfNotGroupArray() == 1) { // any subgroup of at least 1 shift --> NotGroup is a group of activities including REST
		String groupId = p.getNotGroupArray(0);
		return activities.getNotShiftGroup(groupId);
	}
	throw new RuntimeException("unreachable case.");
}

@Override
protected List<ICounter> generateShiftTypeCounters()
{
	List<ICounter> counters = new ArrayList<ICounter>();
	int nbCounters = activities.getNbActivities() - 1;
	for (int a = 0; a < nbCounters; a++) {
		ICounter counter = this.globalCounters.remove("G_" + a);
		if (counter != null) {
			counters.add(counter);
			//} else {
			//	LOGGER.finer("add redundant shiftTypeCounter for activity " + a);
			//	Bounds bounds = Bounds.makeHardBounds(0, periods.getNbDays());
			//	counter = EtpCounter.makeGlobalShiftCounter(periods.getNbDays(), activities.getNbActivities(), new int[]{a}, 1, bounds, 1);
		}
	}
	return counters;
}

@Override
protected ICounter generateShiftCounter()
{
	ICounter counter = this.globalCounters.remove("GS");
	ICounter counterRest = this.globalCounters.remove("G_" + (activities.getNbActivities() - 1));
	if (counter != null) {
		if (counterRest != null) {
			LOGGER.warning(" WARNING ignored: counterShift (only counterRest is used) " + counter + " / " + counterRest);
		}
		return counter;
	}
	SoftBounds bounds;
	if (counterRest != null) {
		bounds = counterRest.bounds().inverseMinMax(periods.getNbDays());
		//} else {
		//	LOGGER.finer("add redundant shiftCounter);
		//	Bounds bounds = Bounds.makeHardBounds(0, periods.getNbDays());
		//}
		return EtpCounter.makeGlobalShiftCounter(periods.getNbDays(), activities.getNbActivities(), activities.getShiftArray(activities.SHIFTS), 1, bounds, 1);
	}
	return null;
}

@Override
protected List<ICounter> generateShiftPerWeekCounters()
{
	//boolean isWeekShift = false;
	//for (ICounter c : weekCounters.values()) {
	//	if (((EtpCounter)c).isWeekShift()) { isWeekShift = true; break; }
	//}
	//if (!isWeekShift) return null;

	List<ICounter> counters = new ArrayList<ICounter>();
	counters.addAll(weekCounters.values());
	weekCounters.clear();
	//int wstart=0;
	//while (wstart < periods.getNbDays()) {
	//	ICounter counter = weekCounters.remove("TCS_"+wstart+"_"+7);
	//	if (counter != null) {
	//		counters.add(counter);
	//		wstart += 7;
	//} else {
	//	int i = wstart+1;
	//	while (i<periods.getNbDays() && weekCounters.get("TCS_"+i+"_"+(i+6))== null)
	//		i++;
	//	Bounds bounds = Bounds.makeHardBounds(0, i-wstart);
	//	counters.add(EtpCounter.makeContinuousShiftCounter(periods.getNbDays(), activities.getNbActivities(), wstart, i-wstart, activities.getShiftArray(activities.SHIFTS), 1, bounds, 1));
	//	LOGGER.finer(" add redundant weekCounter for period " + wstart + " to " + (i-1));
	//	wstart = i;
	//	}
	//}
	LOGGER.finer(counters.size() + " week counters added");
	return counters;
}

protected List<ICounter> generateOtherCounters()
{
	List<ICounter> counters = new ArrayList<ICounter>();
	for (ICounter c : globalCounters.values()) {
		LOGGER.finer(" no linking for global counter :" + c);
		counters.add(c);
	}
	for (ICounter c : weekCounters.values()) {
		LOGGER.finer(" no linking for week counter :" + c);
		counters.add(c);
	}
	for (ICounter c : periodCounters.values()) {
		LOGGER.finer(" no linking for period counter :" + c);
		counters.add(c);
	}
	return counters;
}

}