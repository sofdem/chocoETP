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
 * Date: Feb 28, 2010 - 1:00:59 PM
 */

package etp.data.components;

import choco.automaton.FA.CostAutomaton;
import choco.automaton.FA.IAutomaton;
import choco.automaton.FA.utils.ICounter;
import choco.automaton.bounds.SoftBounds;
import choco.automaton.bounds.SoftBoundsFactory;
import etp.data.EtpCounter;

import java.util.Iterator;
import java.util.List;

/** @author Sophie Demassey */
public class Employee implements Comparable {

private final String id;
private final String name;
private final String contractId;
private int[][] assignmentCost;
private int maxCost;
private IAutomaton automaton;

public Employee(String id, String name, String contractId, int[][] assignmentCost, int maxCost)
{
	this.id = id;
	this.name = name;
	this.contractId = contractId;
	this.assignmentCost = assignmentCost;
	this.maxCost = maxCost;
	this.automaton = null;
}

public Employee(String id, String name, String contractId, int nbPeriods, int nbActivities, int maxCost)
{ this(id, name, contractId, new int[nbPeriods][nbActivities], maxCost); }

public Employee(String id, String name, String contractId, int nbPeriods, int nbActivities)
{ this(id, name, contractId, nbPeriods, nbActivities, Integer.MAX_VALUE / 100); }

public String toString()
{ return "id=" + id + ", name=" + name + ", contract=" + contractId; }

public String getContractId() { return contractId; }

public String getId() { return id; }

public String getName() { return name; }

public int[][] getAssignmentCost() { return assignmentCost; }

public int getAssignmentCost(int period, int activity) { return assignmentCost[period][activity]; }

public void setMaxCost(int maxCost) { if (this.maxCost > maxCost) this.maxCost = maxCost; }

public void setForbidden(int period, int activity)
{ assignmentCost[period][activity] = maxCost + 1; }

public void setForbidden(int period, int activity, int weight)
{
	if (!isForbidden(period, activity) && weight >= 0 && assignmentCost[period][activity] < assignmentCost[period][activity] + weight) {
		assignmentCost[period][activity] += weight;
	} else {
		System.err.println("Warning: not handled assignment cost for e=" + id + ", t=" + period + ", a=" + activity + ", w=" + weight);
	}
}

public void setMandatory(int period, int activity)
{
	int act = 0;
	for (; act < activity; act++) { setForbidden(period, act); }
	for (act = activity + 1; act < assignmentCost[period].length; act++) { setForbidden(period, act); }
}

public void setMandatory(int period, int activity, int weight)
{
	int act = 0;
	for (; act < activity; act++) { setForbidden(period, act, weight); }
	for (act = activity + 1; act < assignmentCost[period].length; act++) { setForbidden(period, act, weight); }
}

public void setMandatory(int period, int[] activities, int weight)
{
	int activity = -1;
	int act = 0;
	for (int i = 0; i < activities.length; i++) {
		assert activity < activities[i] : "the activities must be sorted in increasing order";
		activity = activities[i];
		for (; act < activity; act++) {
			setForbidden(period, act, weight);
		}
		act = activity + 1;
	}
	for (; act < assignmentCost[period].length; act++) {
		setForbidden(period, act, weight);
	}
}


public void setForbiddenWeight(int activity, int weight)
{
	if (weight > 0) {
		for (int period = 0; period < assignmentCost.length; period++) { setForbidden(period, activity, weight); }
	}
}

public void setForbidden(int activity)
{
	for (int period = 0; period < assignmentCost.length; period++) { setForbidden(period, activity); }
}

public boolean isForbidden(int period, int activity)
{ return assignmentCost[period][activity] > maxCost; }

public IAutomaton getAutomaton() { return automaton; }

public void setAutomaton(IAutomaton automaton) { this.automaton = automaton; }

public CostAutomaton getCostAutomaton(boolean aggregateAllIsoCost, boolean withRedundancy)
{
	if (automaton == null) {
		return null;
	}
	CostAutomaton ca = new CostAutomaton((CostAutomaton) automaton);
	SoftBounds boundsAssign = SoftBoundsFactory.makeIdentityPenaltyBounds(maxCost);
	ca.addPrimaryCounter(new EtpCounter(assignmentCost, boundsAssign, "GAC", 1));

	if (aggregateAllIsoCost) {
		List<ICounter> counters = ca.getCounters();
		if (counters.isEmpty()) {
			throw new RuntimeException("the list of counters must be built before aggregation");
		}
		SoftBounds boundsAggreg = SoftBoundsFactory.makeIdentityPenaltyBounds(Integer.MAX_VALUE / 100);
		EtpCounter cag = new EtpCounter(new int[ca.getNbLayers()][ca.getNbSymbols()][ca.getNbStates()], boundsAggreg, "GAG", 1);
		for (Iterator<ICounter> ic = counters.iterator(); ic.hasNext(); ) {
			EtpCounter c = (EtpCounter) ic.next();
			if (c.isCostEqualToCounter()) {
				cag.addWeights(c);
				c.setObjectiveCoefficient(0);
				if (!withRedundancy) ic.remove();
			} else if (c.isCostLinearInCounter()) {
				System.err.println("more efficient to transform the linear penalty on counter in order to get cost=counter");
			}
		}
		ca.addPrimaryCounter(cag);
	}
	return ca;
}

@Override
public int compareTo(Object o) { return this.id.compareTo(((Employee) o).id); }

}
