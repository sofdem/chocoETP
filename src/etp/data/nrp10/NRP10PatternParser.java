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

package etp.data.nrp10;
/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 12/03/11 - 13:53
 */

import choco.automaton.FA.FiniteAutomaton;
import etp.data.PatternParser;
import etp.data.components.Activities;
import etp.data.components.Periods;
import nrp.competition.Patterns;

/** @author Sophie Demassey */
public class NRP10PatternParser extends PatternParser {


/**
 * ******************************************************************************************************
 *
 * Initialization and Parsing of the Unwanted Patterns
 *
 * *******************************************************************************************************
 */

public NRP10PatternParser(Periods periods, Activities activities)
{
	super(periods, activities);
}

/**
 * parse an unwanted pattern and generate either a soft automaton counting the number of times the pattern appears in words
 * or a hard automaton recognizing only the (forbidden) words which contain the pattern
 * @param pattern the unwanted pattern
 * @param isSoft  unwanted pattern treated as a SOFT (true) or HARD (false) constraint
 * @return the automaton
 */
public FiniteAutomaton generateUnwantedPatternAutomaton(Patterns.Pattern pattern, boolean isSoft)
{
	Patterns.Pattern.PatternEntries entries = pattern.getPatternEntries();
	int startDay = this.getPatternStartDay(entries);
	int weight = Integer.parseInt(pattern.getWeight().getStringValue());
	String id = pattern.getID();

	return (isSoft) ? (startDay == 0) ? generateSoftAutomatonFromGeneralPattern(toRegExp(entries), id, weight) : generateSoftAutomatonFromWeeklyPattern(toSequence(entries), startDay, id, weight) : (startDay == 0) ? generateHardAutomatonFromGeneralPattern(toRegExp(entries)) : generateHardAutomatonFromWeeklyPattern(toRegExp(entries), startDay, entries.sizeOfPatternEntryArray());
}


/**
 * check whether a pattern is a weekly pattern, i.e. it should be recognized only if it starts at a given day of the week
 * @param pattern the pattern to be read
 * @return the day of the week (1<>7) for if the pattern is a weekly pattern, or 0 otherwise
 */
private int getPatternStartDay(Patterns.Pattern.PatternEntries pattern)
{
	int startDay = 0;
	for (Patterns.Pattern.PatternEntries.PatternEntry entry : pattern.getPatternEntryArray()) {
		if (!entry.getDay().equals("Any")) {
			int newStartDay = (Periods.getDayOfWeek(entry.getDay()) + 7 - (Integer.parseInt(entry.getIndex().getStringValue()) % 7)) % 7;
			if (startDay == 0) { startDay = newStartDay; } else assert startDay == newStartDay;
		}
	}
	return startDay;
}

/**
 * transform a pattern as a sequence of activityTypes
 * @param pattern the pattern to be read
 * @return the sequence of activityTypes
 */
private String[] toSequence(Patterns.Pattern.PatternEntries pattern)
{
	String[] shifts = new String[pattern.sizeOfPatternEntryArray()];
	for (Patterns.Pattern.PatternEntries.PatternEntry entry : pattern.getPatternEntryArray()) {
		assert entry.isSetIndex();
		int idx = Integer.parseInt(entry.getIndex().getStringValue());
		assert shifts[idx] == null;
		String shiftType = entry.getShiftType();
		shifts[idx] = (shiftType.equals("None")) ? activities.REST : (shiftType.equals("Any")) ? activities.SHIFTS : shiftType;
	}
	return shifts;
}

/**
 * transform a pattern given as a sequence of activityTypes as a regular expression on the set of activities
 * @param pattern the pattern to be read
 * @return the regular expression
 */
protected String toRegExp(Patterns.Pattern.PatternEntries pattern)
{
	String[] shifts = toSequence(pattern);
	StringBuffer b = new StringBuffer();
	for (String a : shifts) {
		b.append(activities.getShiftRegExp(a));
	}
	return b.toString();
}


}
