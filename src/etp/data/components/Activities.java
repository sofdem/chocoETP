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
 * Date: 28/01/11 - 12:59
 */

import choco.kernel.common.util.tools.ArrayUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The list of activities which can be assigned to employees at each period of time
 * we dissociate the SHIFTS (worked activities) from the REST activity (non-worked)
 * each activity is associated to a label (long name), an id (short name) and a value (integer from 0 to nbActivities-1=REST)
 * finally, activities may be appear in groups of activities
 * each group is associated to a label
 * @author Sophie Demassey
 */
public class Activities {

/** the number of activities (including the REST activity indexed by nbActivities - 1) */
private final int nbActivities;

/** the regular expression for "any activity" */
private String anyRegExp;

/** the map for each activity from the label to the index */
private final Map<String, Integer> actIndex;

/** the map for each activity from the index to the label */
private String[] actLabel;

/** list of shift groups */
private Map<String, int[]> shiftGroups;
private Map<String, int[]> shiftArray;

public final String REST = "-";
public final String ALL = "*";
public final String SHIFTS = "$";
private String shiftsSynonym;

/**
 * intialize data from any timetabling instance
 * @param actIndex the mapping of the activity name to the activity index
 */
public Activities(Map<String, Integer> actIndex, String restLabel)
{
	if (restLabel == null || restLabel.isEmpty()) {
		restLabel = REST;
	}
	assert !actIndex.containsKey(REST) && !actIndex.containsValue(actIndex.size());
	actIndex.put(REST, actIndex.size());
	this.actIndex = actIndex;
	this.nbActivities = actIndex.size();
	this.computeImpliedData();
}

/**
 * intialize data from any timetabling instance
 * @param nbActivities the number of activities including Rest
 */
public Activities(int nbActivities)
{
	this.nbActivities = nbActivities;
	this.actIndex = new HashMap<String, Integer>();
	for (int a = 0; a < nbActivities - 1; a++) { actIndex.put(a + "", a); }
	this.computeImpliedData();
}

// TODO: generalize when nbNotShifts > 1
private void computeImpliedData()
{
	shiftArray = new HashMap<String, int[]>();
	this.actLabel = new String[nbActivities];
	for (String act : actIndex.keySet()) {
		assert !act.equals(SHIFTS) && !act.equals(ALL);
		int idx = actIndex.get(act);
		shiftArray.put(act, new int[]{idx});
		actLabel[idx] = act;
	}
	shiftArray.put(ALL, ArrayUtils.zeroToN(nbActivities));
	shiftArray.put(SHIFTS, ArrayUtils.zeroToN(nbActivities - 1));
	this.anyRegExp = this.getRegExp(shiftArray.get(ALL));
}

/**
 * get the number of activities.
 * @return the number of activities
 */
public int getNbActivities()
{ return nbActivities; }

public void initShiftGroups()
{ shiftGroups = new HashMap<String, int[]>(); }

public void addShiftGroup(String groupId, int[] acts)
{
	assert !shiftGroups.containsKey(groupId) : "2 groups with same id";
	assert acts.length < nbActivities;
	assert acts.length >= 1;
	Arrays.sort(acts);
	assert acts[0] >= 0 && acts[acts.length - 1] < nbActivities - 1;
	if (acts.length == nbActivities - 1) {
		assert shiftsSynonym == null;
		shiftsSynonym = groupId;
	}
	shiftGroups.put(groupId, acts);
}

/**
 * get the table of a shift activity index
 * @param actId the shift id or REST or SHIFTS or ALL
 * @return the table of activity [1]
 */
public int[] getShiftArray(String actId)
{ return shiftArray.get(actId); }

public int[] getShiftGroup(String groupId)
{ return shiftGroups.get(groupId); }

public String getShiftRegExp(String shiftId)
{ return shiftId.equals(ALL) ? anyRegExp : getRegExp(getShiftArray(shiftId)); }

public String getShiftGroupRegExp(String groupId)
{ return getRegExp(getShiftGroup(groupId)); }

public String getNotShiftRegExp(String shiftId)
{
	assert !shiftId.equals(ALL);
	return getRegExp(getNotShiftArray(shiftId));
}

public String getNotShiftGroupRegExp(String groupId)
{ return getRegExp(getNotShiftGroup(groupId)); }

public int[] getNotShiftArray(String shiftId)
{
	int[] out = null;
	if (actIndex.containsKey(shiftId)) {
		out = new int[nbActivities - 1];
		int[] in = shiftArray.get(ALL);
		int s = actIndex.get(shiftId);
		System.arraycopy(in, 0, out, 0, s);
		System.arraycopy(in, s + 1, out, s, nbActivities - s - 1);
	} else if (!shiftId.equals(ALL)) {
		int[] not = getShiftArray(shiftId);
		out = new int[nbActivities - not.length];
		int s = 0;
		for (int a = 0; a < nbActivities; a++) {
			if (s < not.length && a == not[s]) { s++; } else out[a - s] = a;
		}
	}
	return out;
}

public int[] getNotShiftGroup(String groupId)
{
	int[] not = getShiftGroup(groupId);
	int[] out = new int[nbActivities - not.length];
	int s = 0;
	for (int a = 0; a < nbActivities; a++) {
		if (s < not.length && a == not[s]) { s++; } else out[a - s] = a;
	}
	return out;
}

private String getRegExp(int activity)
{
	if (activity <= 9) return "" + activity;
	return "<" + activity + ">";
}

private String getRegExp(int[] activities)
{
	if (activities.length == 1) return getRegExp(activities[0]);
	StringBuffer b = new StringBuffer("(").append(getRegExp(activities[0]));
	for (int a = 1; a < activities.length; a++) {
		b.append("|").append(getRegExp(activities[a]));
	}
	b.append(")");
	return b.toString();
}

public Set<String> getActivitySet() { return actIndex.keySet(); }

public String getActivityLabel(int val) { return actLabel[val]; }

/**
 * check if the activity index corresponds to a rest
 * @param act the activity index
 * @return true iff the activity is not a rest
 */
public boolean isNotRestActivity(int act)
{ return act != nbActivities - 1; }

public int getActivityIndex(String shiftId) { return actIndex.get(shiftId); }

public int getRestIndex() { return nbActivities - 1; }

}
