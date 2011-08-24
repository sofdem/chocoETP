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

import choco.automaton.FA.CostAutomaton;
import choco.automaton.FA.FiniteAutomaton;
import choco.automaton.FA.utils.ICounter;
import choco.kernel.common.logging.ChocoLogging;
import etp.data.components.Activities;
import etp.data.components.Periods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}emn.fr
 * Date: Feb 28, 2010 - 7:38:19 PM
 */

/** @author Sophie Demassey */
public abstract class AutomatonBuilder {

protected final Logger LOGGER = ChocoLogging.getMainLogger();
protected final Periods periods;
protected final Activities activities;

protected AutomatonBuilder(Periods periods, Activities activities)
{
	this.periods = periods;
	this.activities = activities;
	LOGGER.finer("PARSER " + activities.getNbActivities() + " activities, " + periods.getNbDays() + " periods");
}


public Map<String, FiniteAutomaton> buildAutomata()
{
//	public Map<String, FiniteAutomaton> buildAutomata(boolean useGlobalAssignmentCounter, boolean useIndividualAssignmentCounter) {
	//todo generate 1 or several automata depending of properties
	Map<String, FiniteAutomaton> automata = new HashMap<String, FiniteAutomaton>();
	automata.put("mca", generateAggregatedCostAutomaton());
	return automata;
}

/**
 * build one automaton with multiple counters recognizing only the sequences of activities permitted by the contract rules
 * each counter is a quantitative and additive criterion, defined by the contract rules, for evaluating any word recognized by the automaton
 * @return the multi-counter automaton
 */
protected CostAutomaton generateAggregatedCostAutomaton()
//protected CostAutomaton generateAggregatedCostAutomaton(boolean useGlobalAssignmentCounter, boolean useIndividualAssignmentCounter)
{
	FiniteAutomaton automaton = this.generateAggregatedHardAutomaton();
	List<ICounter> counters = new ArrayList<ICounter>();
	//if (useGlobalAssignmentCounter)
	//	counters.add(this.generateGlobalAssignmentCostCounter());
	//if (useIndividualAssignmentCounter)
	//	counters.add(this.generateGlobalAssignmentRequestCounter());
	counters.addAll(generateAllShiftCounters());
	// return new CostAutomaton(automaton, counters, periods.getNbDays(), activities.getNbActivities());
	return new CostAutomaton(automaton, counters);
}

/**
 * build a minimized automaton recognizing only the sequences of activities permitted by the contract rules
 * @return the minimized DFA
 */
protected abstract FiniteAutomaton generateAggregatedHardAutomaton();

/**
 * generate all the counters associated to a contract.
 * The first counter has a special status as it corresponds to the objective function in the MCR constraint.
 * nbDaysPerShiftType, nbShiftsPerWeek, nbShifts, nbWE, nbWEShifts
 * counters of Rest are automatically convert as counters of Shifts
 * @return the list of counters
 */
protected List<ICounter> generateAllShiftCounters()
{
	List<ICounter> counters = new ArrayList<ICounter>();
	List<ICounter> ac;
	ac = this.generateShiftTypeCounters();
	if (ac != null) counters.addAll(ac);
	ac = this.generateShiftPerWeekCounters();
	if (ac != null) counters.addAll(ac);

	ICounter c;
	c = this.generateShiftCounter();
	if (c != null) counters.add(c);
	c = this.generateWECounter();
	if (c != null) counters.add(c);
	c = this.generateWEShiftCounter();
	if (c != null) counters.add(c);

	ac = this.generateOtherCounters();
	if (ac != null) counters.addAll(ac);

	return counters;
}

/**
 * generate a counter per week: min/max number of worked shifts in a week.
 * both bounds are soft; hard bounds = [0,7]
 * @return the list of counters [<=nbWeeks]
 */
protected List<ICounter> generateShiftPerWeekCounters()
{ return null; }

/**
 * generate a counter per shift type: min/max number of occurrences of a shift type.
 * only max bound is soft; hard bounds = [minDaysShiftType, nbDays]
 * @return the list of counters [<=nbActivities]
 */
protected abstract List<ICounter> generateShiftTypeCounters();

/**
 * generate a counter: min/max number of worked shifts.
 * only max bound is soft; hard bounds = [0, nbDays]
 * @return the counter object
 */
protected abstract ICounter generateShiftCounter();

/**
 * generate a counter: min/max number of worked week-ends.
 * both bounds are soft; hard bounds = [0, nbWeeks]
 * @return the counter object
 */
protected ICounter generateWECounter()
{ return null; }

/**
 * generate a counter: min/max number of worked week-end days.
 * both bounds are soft; hard bounds: [0, nbWEDays]
 * @return the counter object
 */
protected ICounter generateWEShiftCounter()
{return null; }

///**
// * generate a counter: the sum of the period-activity assignment costs.
// * @return the counter object
// */
//protected Counter generateGlobalAssignmentCostCounter()
//{
//	CounterType type = new CounterType(CounterType.Class.ASSIGN_COST, "");
//	AbstractBoundedPenaltyFunction penalty = new IsoPenaltyFunction();
//	LOGGER.finer("counter: global cost");
//	return new Counter(type, penalty);
//}
//
///**
// * generate a counter: the sum of the period-activity assignment request weights.
// * @return the counter object
// */
//protected Counter generateGlobalAssignmentRequestCounter()
//{
//	CounterType type = new CounterType(CounterType.Class.ASSIGN_REQ, "");
//	AbstractBoundedPenaltyFunction penalty = new IsoPenaltyFunction();
//	LOGGER.finer("counter: assignment request");
//	return new Counter(type, penalty);
//}

protected List<ICounter> generateOtherCounters() { return null; }

}
