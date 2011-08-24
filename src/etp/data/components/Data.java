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
/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 28/01/11 - 14:52
 */

import choco.automaton.FA.CostAutomaton;
import choco.automaton.FA.IAutomaton;

import java.util.Map;

//import etp.components.automata.CostAutomaton;

/** @author Sophie Demassey */
public class Data {

/** the name of the instance */
protected final String problemName;
//protected final int nbEmployees;   // todo to remove
protected final Activities activities;
protected final Periods periods;

protected Employee[] employees;
protected Cover globalCover;
protected Map<String, SkillCover> skillCovers;
//protected Map<String, FiniteAutomaton> automata;

public Data(String problemName, Activities activities, Periods periods)
{
	this.problemName = problemName;
	this.activities = activities;
	this.periods = periods;
}

public void setEmployees(Employee[] employees) { this.employees = employees; }

public void setGlobalCover(Cover globalCover) { this.globalCover = globalCover; }

public void setSkillCovers(Map<String, SkillCover> skillCovers) { this.skillCovers = skillCovers; }
//public void setAutomata(Map<String, FiniteAutomaton> automata) { this.automata = automata; }

public String getProblemName() { return problemName; }

public int getNbEmployees() { return employees.length; }

public Activities getActivities() { return activities; }

public Periods getPeriods() { return periods; }

public Employee[] getEmployees() { return employees; }

public Cover getGlobalCover() { return globalCover; }

public Map<String, SkillCover> getSkillCovers() { return skillCovers; }

public void setEmployeeMaxAssignmentCost(int employee, int maxCost) { employees[employee].setMaxCost(maxCost);}

public boolean isForbidden(int employee, int period, int activity)
{
	return employees[employee].isForbidden(period, activity);
}

public int getAssignmentCost(int employee, int period, int activity)
{
	return employees[employee].getAssignmentCost(period, activity);
}

//public FiniteAutomaton getAutomaton(int employee) { return automata.get(employees[employee].getContractId()); }
public IAutomaton getAutomaton(int employee)
{ return employees[employee].getAutomaton(); }

//public CostAutomaton getCostAutomaton(int employee, boolean aggregateAllIsoCosts) { return employees[employee].getCostAutomaton(aggregateAllIsoCosts); }
public CostAutomaton getCostAutomaton(int employee, boolean aggregateAllIsoCost, boolean withRedundancy)
{ return employees[employee].getCostAutomaton(aggregateAllIsoCost, withRedundancy); }
//public CostAutomaton getCostAutomaton(int employee) {
//	CostAutomaton automaton = (CostAutomaton)getAutomaton(employee);
//	automaton.setAssignmentRequestWeights(employees[employee].getAssignmentCost());
//	return automaton;
//}

public String[] getEmployeeIds()
{
	String[] ids = new String[employees.length];
	for (int e = 0; e < employees.length; e++) {
		ids[e] = employees[e].getId();
	}
	return ids;
}


}
