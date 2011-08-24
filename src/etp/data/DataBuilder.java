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

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: Nov 19, 2010 - 2:12:02 PM
 */


import choco.automaton.FA.FiniteAutomaton;
import etp.data.components.*;

import java.util.Map;
import java.util.Properties;

/** @author Sophie Demassey */
public abstract class DataBuilder {

protected Data data;
protected DataProperties properties;

protected class DataProperties {
	//boolean useGlobalAssignmentCounter;
	//boolean useIndividualAssignmentCounter;
	public final int hardSoftLimit;

	public DataProperties(Properties properties)
	{
		this.hardSoftLimit = Integer.parseInt(properties.getProperty("model.hard.soft.limit"));
		//this.useGlobalAssignmentCounter = Boolean.parseBoolean(properties.getProperty("automaton.counter.assignment.global"));
		//this.useIndividualAssignmentCounter = Boolean.parseBoolean(properties.getProperty("automaton.counter.assignment.individual"));
	}

	@Override
	public String toString()
	{
		return "AutomatonProperties{" + "hard/soft limit=" + hardSoftLimit
				//+ "useGlobalAssignmentCounter=" + useGlobalAssignmentCounter
				//+ ", useIndividualAssignmentCounter=" + useIndividualAssignmentCounter
				+ '}';
	}
}


public DataBuilder(Properties properties)
{
	this.properties = new DataProperties(properties);
}

public Data getData() { return data; }

public void buildData()
{
	data = new Data(this.readProblemName(), this.buildActivities(), this.buildPeriods());
	data.setEmployees(this.buildEmployees());
	data.setGlobalCover(this.buildGlobalCover());
	data.setSkillCovers(this.buildSkillCovers());
	this.buildAutomata();
	for (Employee e : data.getEmployees()) {
		e.setAutomaton(this.getAutomaton(e));
	}
}

protected abstract String readProblemName();

protected abstract Activities buildActivities();

protected abstract Periods buildPeriods();

protected abstract Employee[] buildEmployees();

protected abstract Cover buildGlobalCover();

protected abstract Map<String, SkillCover> buildSkillCovers();

protected abstract void buildAutomata();

protected abstract FiniteAutomaton getAutomaton(Employee e);
}
