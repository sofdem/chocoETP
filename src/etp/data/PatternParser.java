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

package etp.data;/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 12/03/11 - 14:07
 */

import choco.automaton.FA.CostAutomaton;
import choco.automaton.FA.FiniteAutomaton;
import choco.automaton.FA.ICostAutomaton;
import choco.automaton.bounds.SoftBounds;
import choco.automaton.bounds.SoftBoundsFactory;
import etp.data.components.Activities;
import etp.data.components.Periods;

import java.util.ArrayList;

/** @author Sophie Demassey */
public class PatternParser {

protected final Periods periods;
protected final Activities activities;
private final String any;


public PatternParser(Periods periods, Activities activities)
{
	this.periods = periods;
	this.activities = activities;
	this.any = activities.getShiftRegExp(activities.ALL);
}

enum PatternType {
	GENERAL_SLIDING
			{
				String prefix(String any, int... t) { return any + "*"; }

				String getRegexp(String regExp, String any, int... t) { return prefix(any) + suffix(regExp, any); }

				String toString(String regExp, int weight, int... t)
				{
					return isSoft(weight) + " general pattern " + regExp;
				}

				String getSoftRegexp(String regExp, String any, int alpha, int... t)
				{
					return "(" + any + "*(" + regExp + "<" + alpha + ">*)*)*";
				}
			},
	FROM_SLIDING
			{
				public String prefix(String any, int... t) { return any + "{" + t[0] + "}" + any + "*"; }

				String getRegexp(String regExp, String any, int... t) { return prefix(any, t) + suffix(regExp, any); }

				String toString(String regExp, int weight, int... t)
				{
					return isSoft(weight) + " general pattern " + regExp + " from " + t[0];
				}

				String getSoftRegexp(String regExp, String any, int alpha, int... t)
				{
					return any + "{" + t[0] + "}" + "(" + any + "*(" + regExp + "<" + alpha + ">*)*)*";
				}
			},
	AT_FIXED
			{
				String prefix(String any, int... t) { return any + "{" + t[0] + "}"; }

				String getRegexp(String regExp, String any, int... t) { return prefix(any, t) + suffix(regExp, any); }

				String toString(String regExp, int weight, int... t)
				{
					return isSoft(weight) + " general pattern " + regExp + " at " + t[0];
				}

				String getSoftRegexp(String regExp, String any, int alpha, int... t)
				{
					return prefix(any, t) + "((" + regExp + "<" + alpha + ">*)|" + any + "*)" + any + "*";
				}
			},
	AT_PERIODIC
			{
				// @return the regular expression (A{i}|A{i+p}|A{i+2p}|...|A{i+(n-1)p}) denoting the language where A denotes any symbol and P the pattern
				String prefix(String any, int... t)
				{
					int start = t[0];
					StringBuffer s = new StringBuffer("(").append(AT_FIXED.prefix(any, start));
					while (start + t[1] + t[2] <= t[3]) {
						start += t[2];
						s.append("|").append(AT_FIXED.prefix(any, start));
					}
					s.append(")");
					return s.toString();
				}

				String getRegexp(String regExp, String any, int... t) { return prefix(any, t) + suffix(regExp, any); }

				String toString(String regExp, int weight, int... t)
				{
					return isSoft(weight) + " weekly pattern " + regExp + " at " + t[0];
				}

				String getSoftRegexp(String regExp, String any, int alpha, int... t)
				{
					return prefix(any, t) + "((" + regExp + "<" + alpha + ">*)|" + any + "*)" + any + "*";
				}
			};

	String suffix(String regExp, String any) { return regExp + any + "*"; }

	String isSoft(int weight) { return weight != 0 ? "soft " + weight : "hard"; }

	abstract String prefix(String any, int... tmp);

	abstract String toString(String regExp, int weight, int... tmp);

	abstract String getRegexp(String regExp, String any, int... t);

	String getSoftRegexp(String regExp, String any, int alpha, int... t) { return null; }
}

private FiniteAutomaton generateHardAutomatonFromGeneralPattern(PatternType type, String regExp, int... t)
{
	String hardRegExp = type.getRegexp(regExp, any, t);
	System.out.println(type.toString(regExp, 0, t) + "  --H->   " + hardRegExp);
	return new FiniteAutomaton(hardRegExp);
}

public FiniteAutomaton generateHardAutomatonFromGeneralPattern(String regExp)
{ return generateHardAutomatonFromGeneralPattern(PatternParser.PatternType.GENERAL_SLIDING, regExp); }

public FiniteAutomaton generateHardAutomatonFromGeneralPattern(String regExp, int regionStart)
{ return generateHardAutomatonFromGeneralPattern(PatternParser.PatternType.FROM_SLIDING, regExp, regionStart); }

public FiniteAutomaton generateHardAutomatonFromGeneralFixedPattern(String regExp, int startDay)
{ return generateHardAutomatonFromGeneralPattern(PatternParser.PatternType.AT_FIXED, regExp, startDay); }

public FiniteAutomaton generateHardAutomatonFromWeeklyPattern(String regExp, int startDay, int lengthExp)
{ return generateHardAutomatonFromGeneralPattern(PatternParser.PatternType.AT_PERIODIC, regExp, startDay, lengthExp, 7, periods.getNbDays()); }

public CostAutomaton generateSoftAutomatonFromGeneralPattern(PatternType type, String regExp, String patternId, int weight, int... t)
{
	int alpha = activities.getNbActivities() + 2;

	String softRegExp = type.getSoftRegexp(regExp, any, alpha, t);
	System.out.println(type.toString(regExp, weight, t) + "  --S->   " + softRegExp);

	CostAutomaton fa = new CostAutomaton(softRegExp);
	fa.minimize();

	ArrayList<int[]> toPenalise = fa._removeSymbolFromAutomaton(alpha);
	int[][][] weights = makeWeightTable(fa);
	for (int[] p : toPenalise) {
		for (int i = 0; i < weights.length; i++) {
			weights[i][p[1]][p[0]] = weight;
		}
	}
	//CounterType cardType = new CounterType(CounterType.Class.PATTERN, "P"+patternId);
	//AbstractBoundedPenaltyFunction cardBound = new IsoPenaltyFunction(weight*periods.getNbDays());
	SoftBounds bounds = SoftBoundsFactory.makeIdentityPenaltyBounds(weight * periods.getNbDays());
	fa.addCounter(new EtpCounter(weights, bounds, "P" + patternId, 1));
	return fa;
}


public CostAutomaton generateSoftAutomatonFromGeneralPattern(String regExp, String patternId, int weight)
{ return generateSoftAutomatonFromGeneralPattern(PatternType.GENERAL_SLIDING, regExp, patternId, weight); }

public CostAutomaton generateSoftAutomatonFromGeneralPattern(String regExp, String patternId, int regionStart, int weight)
{ return generateSoftAutomatonFromGeneralPattern(PatternType.FROM_SLIDING, regExp, patternId, weight, regionStart); }

public CostAutomaton generateSoftAutomatonFromGeneralFixedPattern(String regExp, String patternId, int startDay, int weight)
{ return generateSoftAutomatonFromGeneralPattern(PatternType.AT_FIXED, regExp, patternId, weight, startDay); }

public CostAutomaton generateSoftAutomatonFromWeeklyPattern(String regExp, String patternId, int startDay, int lengthExp, int weight)
{ return generateSoftAutomatonFromGeneralPattern(PatternType.AT_PERIODIC, regExp, patternId, weight, startDay, lengthExp, 7, periods.getNbDays()); }
//{
//System.out.println("not implemented soft fixed pattern " + regExp + " (" + patternId + ") at " + startDay + " : " + weight);
//return null;
//}


// todo: use the removeSymbolFromAutomaton trick
// todo: marche probablement pas avec ASAP3 (notShiftGroup)  use a matrix actTypes[i][j]=1 iff j est un symbole du motif a la position j

/**
 * build the week layered automaton for unwanted pattern P of length p < 7.
 * the automaton has 7 layers corresponding each to a day, beginning from the start day of P (layer 0)
 * each layer d contains 1 state and 1 'all'-transition to the state in the next layer (%7), except:
 * layer d, for 1 <= d < p, contains 2 states [inP and notInP]
 * only transitions at the end of the pattern from layerState[p-1][inP] to layerState[p][0] have a cost
 * @param actTypes  the pattern as a sequence of shiftTypes
 * @param patternId the pattern name
 * @return pattern the automaton to build
 */
public CostAutomaton generateSoftAutomatonFromWeeklyPattern(String[] actTypes, int startDay, String patternId, int weight)
{
	System.out.println("soft weekly pattern at " + startDay + " (" + patternId + ")");
	int pStart = periods.getFirstIndexForDayOfWeek(startDay);
	int pLength = actTypes.length;
	assert pLength < 7;
	int firstDay = (pStart == 0) ? 0 : 7 - pStart;
	int lastDay = (firstDay + periods.getNbDays()) % 7;
	int[] all = activities.getShiftArray(activities.ALL);

	CostAutomaton pattern = new CostAutomaton();
	int[][] layerStates = new int[7][];
	int d = 0;
	layerStates[d++] = this.addStates(pattern, 1);
	for (; d < pLength; d++) {
		String shift = actTypes[d - 1];
		layerStates[d] = this.addStates(pattern, 2);
		pattern.addTransition(layerStates[d - 1][0], layerStates[d][0], activities.getShiftArray(shift));  // todo generalize to NotShiftGroup...
		if (!shift.equals(activities.ALL)) {
			pattern.addTransition(layerStates[d - 1][0], layerStates[d][1], activities.getNotShiftArray(shift));
		}
		if (layerStates[d - 1].length == 2) pattern.addTransition(layerStates[d - 1][1], layerStates[d][1], all);
	}
	for (; d < 7; d++) {
		layerStates[d] = this.addStates(pattern, 1);
		pattern.addTransition(layerStates[d - 1][0], layerStates[d][0], all);
	}
	pattern.addTransition(layerStates[6][0], layerStates[0][0], all);
	if (layerStates[pLength - 1].length == 2) {
		pattern.addTransition(layerStates[pLength - 1][1], layerStates[pLength][0], all);
	}

	pattern.setInitialState(layerStates[firstDay][layerStates[firstDay].length - 1]);
	for (int s : layerStates[lastDay]) pattern.setFinal(s);

	int q = layerStates[pLength - 1][0];
	int[][][] weights = makeWeightTable(pattern);
	for (int t = pStart + pLength - 1; t < weights.length; t += 7) {
		for (int a : activities.getShiftArray(actTypes[actTypes.length - 1])) {
			weights[t][a][q] = weight;
		}
	}
	//CounterType cardType = new CounterType(CounterType.Class.PATTERN, "P_" + startDay +patternId);
	//AbstractBoundedPenaltyFunction cardBound = new IsoPenaltyFunction(weight*periods.getNumberOfIndexForDayOfWeek(pStart));
	SoftBounds bounds = SoftBoundsFactory.makeIdentityPenaltyBounds(weight * periods.getNumberOfIndexForDayOfWeek(pStart));
	pattern.addCounter(new EtpCounter(weights, bounds, "P" + patternId + "_" + startDay, 1));
	return pattern;

}

public CostAutomaton buildNoNightBeforeFreeWEAutomaton(int lengthWE, int firstWEDay, int weight)
{
	assert activities.getShiftArray("N") != null : "no night shift defined";
	String[] shifts = new String[lengthWE + 1];
	int idx = 0;
	shifts[idx++] = "N";
	for (; idx < shifts.length; idx++) { shifts[idx] = activities.REST; }
	int startDay = (firstWEDay == 1) ? 7 : firstWEDay - 1;
	return generateSoftAutomatonFromWeeklyPattern(shifts, startDay, "NRR", weight);
}

/**
 * build the WE layered automaton.
 * the automaton has 7 layers corresponding each to a day, beginning from the first day in the week-end (layer 0)
 * each layer d contains 1 state (layerState[d][0]) and 1 'all'-transition to the state in the next layer (%7), except:
 * layer 1 contains nbActivities states and layer d, for 2 <= d < lengthWE , contains nbActivities+2 states, one for each WE type:
 * layerState[d][a] (worked complete identical=a)  for all shift a, layerState[d][R] (not-worked complete identical=R),
 * layerState[d][R+1] (worked complete not-identical) and layerState[d][nbActivities+1] (worked not-complete not-identical)
 * only the transitions in layer d=lenghtWE are weighted according to the type of their initial state
 * @return pattern the automaton to build
 */
public CostAutomaton buildWEAutomaton(int lengthWE, int startWE)
{
	int firstDayState = (startWE == 0) ? 0 : 7 - startWE;
	int lastDayState = (firstDayState + periods.getNbDays()) % 7;
	assert lengthWE < 7;
	assert firstDayState == 0 || firstDayState >= lengthWE;
	assert lastDayState == 0 || lastDayState >= lengthWE;

	int[] all = activities.getShiftArray(activities.ALL);

	CostAutomaton pattern = new CostAutomaton();
	int[][] layerStates = new int[7][];

	layerStates[0] = this.addStates(pattern, 1);
	layerStates[1] = this.addStates(pattern, activities.getNbActivities());
	for (int a : all) { pattern.addTransition(layerStates[0][0], layerStates[1][a], a); }
	for (int d = 2; d < lengthWE; d++) {
		layerStates[d] = this.addStates(pattern, activities.getNbActivities() + 2);
		this.addWETransitions(pattern, layerStates[d - 1], layerStates[d]);
	}
	layerStates[lengthWE] = this.addStates(pattern, 1);
	for (int s : layerStates[lengthWE - 1]) { pattern.addTransition(s, layerStates[lengthWE][0], all); }
	for (int d = lengthWE + 1; d < 7; d++) {
		layerStates[d] = this.addStates(pattern, 1);
		pattern.addTransition(layerStates[d - 1][0], layerStates[d][0], all);
	}
	pattern.addTransition(layerStates[6][0], layerStates[0][0], all);

	pattern.setInitialState(layerStates[firstDayState][0]);
	pattern.setFinal(layerStates[lastDayState][0]);
	return pattern;
}

protected int[][][] makeWeightTable(ICostAutomaton automaton)
{ return new int[periods.getNbDays()][activities.getNbActivities()][automaton.getNbStates()]; }


/**
 * create and add a given number of states as a new layer in the WE layered automaton.
 * @param pattern  the automaton to build
 * @param nbStates the nb states to create
 * @return the list of indexes of the created states [nbStates]
 */
protected int[] addStates(FiniteAutomaton pattern, int nbStates)
{
	int[] layer = new int[nbStates];
	for (int i = 0; i < nbStates; i++) {
		pattern.addState();
		layer[i] = pattern.getNbStates() - 1;
	}
	return layer;
}

/*
 * create and add the transitions of a layer corresponding to an inner week-end day in the WE automaton.
 * a transition from state layer1[q] with label a enters a state layer2[q']: q' depends on (a,q) as follows:
 * q' = a (worked complete identical=a) if (a, a) for all shift a
 * q' = R (not-worked complete identical=R) if (R, R)
 * q' = R+1 (worked complete not-identical) if (a, b) or (a, R+1)  for all shift a != b
 * q' = R+2 (worked not-complete not-identical) otherwise (if (R, a) or (a, R) or (R, R+1) or (R, R+2) or (a, R+2)  for all shift a)
 * @param pattern the automaton to build
 * @param layer1 the states in the current layer [nbAct or nbAct+2]
 * @param layer2 the states in the next layer [nbAct+2]
 */
private void addWETransitions(FiniteAutomaton pattern, int[] layer1, int[] layer2)
{
	int[] shifts = activities.getShiftArray(activities.SHIFTS);
	int rest = activities.getRestIndex();
	int compState = rest + 1;
	int notCompState = rest + 2;
	for (int a : shifts) {
		pattern.addTransition(layer1[a], layer2[a], a);
		for (int b = 0; b < a; b++) {
			pattern.addTransition(layer1[a], layer2[compState], b);
			pattern.addTransition(layer1[b], layer2[compState], a);
		}
		pattern.addTransition(layer1[a], layer2[notCompState], rest);
		pattern.addTransition(layer1[rest], layer2[notCompState], a);
	}
	pattern.addTransition(layer1[rest], layer2[rest], rest);

	if (layer1.length == activities.getNbActivities() + 2) {
		pattern.addTransition(layer1[compState], layer2[compState], shifts);
		pattern.addTransition(layer1[compState], layer2[notCompState], rest);
		pattern.addTransition(layer1[notCompState], layer2[notCompState], shifts);
		pattern.addTransition(layer1[notCompState], layer2[notCompState], rest);
	}
}


}
