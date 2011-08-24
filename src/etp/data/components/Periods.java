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

package etp.data.components;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}emn.fr
 * Date: Feb 27, 2010 - 7:08:40 PM
 */


/** @author Sophie Demassey */
public class Periods {

/** the number of planning periods */
private final int nbDays;

/** the day of the week of the first planning period */
private final Calendar startDay;

/** the list of employees */
//private List<Employee> employees;

/** the list of skills and associated covers: the first one is the global cover over all employees */
//private List<Skill> skills;

/** list of automata with counters modeling the contract rules [nbContract] */
//private Map<String, FiniteAutomaton> automata;

/**
 * intialize data from any timetabling instance
 * @param startDay the first scheduling day
 * @param endDay   the last scheduling day
 */
public Periods(Calendar startDay, Calendar endDay)
{
	if (Periods.mapDays == null) Periods.setMapDays();
	this.startDay = startDay;
	this.nbDays = this.getDayIndex(endDay) + 1;
}

/**
 * intialize data from any timetabling instance
 * @param nbDays the number of periods
 */
public Periods(int nbDays)
{
	this.nbDays = nbDays;
	this.startDay = null;
}

/**
 * get the number of days.
 * @return the number of days
 */
public int getNbDays()
{ return nbDays; }

public Calendar getStartDay() { return (Calendar) startDay.clone(); }

//public int getNbWeeks() { assert nbDays % 7 == 0: "the number of days " + nbDays + " is not a multiple of 7"; return nbDays / 7; }

public int getDayIndex(Calendar date)
{ return Math.round(date.getTimeInMillis() / (1000 * 60 * 60 * 24)) - Math.round(startDay.getTimeInMillis() / (1000 * 60 * 60 * 24)); }

public int getPeriodIndex(Calendar date, BigInteger day) { return (date != null) ? getDayIndex(date) : day.intValue(); }


private static Map<String, Integer> mapDays;

private static void setMapDays()
{
	mapDays = new HashMap<String, Integer>();
	mapDays.put("SUNDAY", Calendar.SUNDAY);
	mapDays.put("MONDAY", Calendar.MONDAY);
	mapDays.put("TUESDAY", Calendar.TUESDAY);
	mapDays.put("WEDNESDAY", Calendar.WEDNESDAY);
	mapDays.put("THURSDAY", Calendar.THURSDAY);
	mapDays.put("FRIDAY", Calendar.FRIDAY);
	mapDays.put("SATURDAY", Calendar.SATURDAY);
}

public static int getDayOfWeek(String dayOfWeek)
{
	Integer i = mapDays.get(dayOfWeek.toUpperCase());
	if (i == null) throw new IllegalArgumentException(dayOfWeek + " is not a day of week.");
	return i;
}

public int getFirstIndexForDayOfWeek(String dayOfWeek)
{
	int i = getDayOfWeek(dayOfWeek);
	int s = startDay.get(Calendar.DAY_OF_WEEK);
	return (i >= s) ? i - s : i + 7 - s;
}

public int getFirstIndexForDayOfWeek(int dayOfWeek)
{
	assert dayOfWeek >= 1 && dayOfWeek <= 7;
	int start = startDay.get(Calendar.DAY_OF_WEEK);
	return (dayOfWeek >= start) ? dayOfWeek - start : dayOfWeek + 7 - start;
}


public int getNumberOfIndexForDayOfWeek(int firstIndex)
{
	assert firstIndex < nbDays;
	return (nbDays - 1 - firstIndex) / 7 + 1;
}

public int[] getAllIndexesForDayOfWeek(String dayOfWeek)
{
	int idx = getFirstIndexForDayOfWeek(dayOfWeek);
	int[] out = new int[getNumberOfIndexForDayOfWeek(idx)];
	for (int i = 0; i < out.length; i++, idx += 7) { out[i] = idx; }
	return out;
}

/**
 * get the table of the period indexes of all sundays
 * @return the table of periods [nbWeeks]
 */
public int[] getSundayIndexes()
{ return this.getAllIndexesForDayOfWeek("SUNDAY"); }

/*
public void setSkills(List<Skill> skills)
{
	assert skills.get(0).getEmployees().length == nbEmployees;
	this.skills = skills;
}

public List<Skill> getAllSkills() { return skills; }
public List<Skill> getSubSkills() { return skills.subList(1,skills.size()); }
public Cover getGlobalCover() { return skills.get(0).getCover(); }


public void setEmployees(List<Employee> employees)
{
	assert employees.size() == nbEmployees;
	this.employees = employees;
}

public Employee getEmployee(int index) { return employees.get(index); }

public List<Employee> getEmployees() { return employees; }

public FiniteAutomaton getAutomaton(String contractId) { return automata.get(contractId); }

public void setAutomata(Map<String, FiniteAutomaton> automata) { this.automata = automata; }

public Iterable<? extends FiniteAutomaton> getAutomata() { return automata.values(); }

*/
}