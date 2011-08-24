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

package etp.data.asap3;

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 13/03/11 - 01:16
 */

import asap3.Pattern;
import choco.automaton.FA.FiniteAutomaton;
import etp.data.PatternParser;
import etp.data.components.Activities;
import etp.data.components.Periods;
import org.apache.xmlbeans.XmlCursor;

/** @author Sophie Demassey */
public class ASAP3PatternParser extends PatternParser {


public ASAP3PatternParser(Periods periods, Activities activities)
{
	super(periods, activities);
}

/**
 * parse an unwanted pattern and generate either a soft automaton counting the number of times the pattern appears in words
 * or a hard automaton recognizing only the (forbidden) words which contain the pattern
 * @param pattern the unwanted pattern
 * @return the automaton
 */
public FiniteAutomaton generateUnwantedPatternAutomaton(Pattern pattern, int matchStart, int weight)
{
	int startDay = getStartPeriod(pattern);
	if (weight == 0) {
		if (startDay < 0) {
			return (matchStart == 0) ? generateHardAutomatonFromGeneralPattern(toRegExp(pattern)) : generateHardAutomatonFromGeneralPattern(toRegExp(pattern), matchStart);
		}
		if (!pattern.isSetStartDay()) {
			assert (startDay >= matchStart) : "pattern starts before match ! ";
			return generateHardAutomatonFromGeneralFixedPattern(toRegExp(pattern), startDay);
		}
		while (startDay < matchStart) startDay += 7;
		if (startDay >= periods.getNbDays()) throw new RuntimeException("no period match for week counter " + pattern);
		return generateHardAutomatonFromWeeklyPattern(toRegExp(pattern), startDay, getSize(pattern));
	}
	if (startDay < 0) {
		return (matchStart == 0) ? generateSoftAutomatonFromGeneralPattern(toRegExp(pattern), "", weight) : generateSoftAutomatonFromGeneralPattern(toRegExp(pattern), "", matchStart, weight);
	}
	if (!pattern.isSetStartDay()) {
		assert (startDay >= matchStart) : "pattern starts before match ! ";
		return generateSoftAutomatonFromGeneralFixedPattern(toRegExp(pattern), "", startDay, weight);
	}
	while (startDay < matchStart) startDay += 7;
	if (startDay >= periods.getNbDays()) throw new RuntimeException("no period match for week counter " + pattern);
	return generateSoftAutomatonFromWeeklyPattern(toRegExp(pattern), "", startDay, getSize(pattern), weight);

	//System.out.println("no automaton for " + toRegExp(pattern));
	//return null;
}

private String toRegExp(Pattern pattern)
{
	XmlCursor cursor = pattern.newCursor();
	cursor.toFirstChild();
	//System.out.println("0: " + cursor.getName());
	while (cursor.getName().getLocalPart().contains("Start")) {
		cursor.toNextSibling();
	}
	StringBuffer b = new StringBuffer();
	do {
		b.append(toRegExp(cursor));
	} while (cursor.toNextSibling());
	return b.toString();
}

private String toRegExp(XmlCursor cursor)
{
	String name = cursor.getName().getLocalPart();
	String text = cursor.getTextValue();
	//System.out.println("1: " + cursor.getName() + " = " + text);
	if (name.equals("Shift")) return activities.getShiftRegExp(text);
	if (name.equals("ShiftGroup")) return activities.getShiftGroupRegExp(text);
	if (name.equals("NotShift")) return activities.getNotShiftRegExp(text);
	if (name.equals("NotGroup")) return activities.getNotShiftGroupRegExp(text);
	throw new RuntimeException(name + ": not implemented in RegExp.");
}

int getStartPeriod(Pattern pattern)
{
	if (pattern.isSetStart()) return pattern.getStart().intValue();
	if (pattern.isSetStartDate()) return periods.getDayIndex(pattern.getStartDate());
	if (pattern.isSetStartDay()) return periods.getFirstIndexForDayOfWeek(pattern.getStartDay());
	return -1;
}

int getSize(Pattern p)
{ return p.sizeOfNotGroupArray() + p.sizeOfNotShiftArray() + p.sizeOfShiftArray() + p.sizeOfShiftGroupArray(); }


}
