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
 * User: sofdem - sophie.demassey{at}emn.fr
 * Date: Jan 7, 2010 - 11:47:49 AM
 */

/**
 * A cover requirement: the numbers of employees required for all periods and activities.
 * These numbers are specified using hard (mandatory) and soft (preferential) bounds.
 * @author Sophie Demassey
 */
public class Cover {

/** maximum cover value: number of covered employees */
private final int coverMaxValue;

// todo: SOFTBOUNDs
/** cover lower bound: default = 0 */
private final int[][] coverLB;

/** cover upper bound: default = coverMaxValue */
private final int[][] coverUB;

/** cover lower bound: default = coverLB */
private final int[][] coverSoftLB;

/** cover lower bound: default = coverUB */
private final int[][] coverSoftUB;

/** cover requirement label */
private final String label;

/**
 * get the label
 * @return the label
 */
public String getLabel()
{ return label; }

/**
 * get the cover lower bound for a period and an activity
 * @param period   the period
 * @param activity the activity
 * @return the cover lower bound
 */
public int getLB(int period, int activity)
{ return coverLB[period][activity]; }

/**
 * get the cover lower bound for a period and an activity
 * @param period   the period
 * @param activity the activity
 * @return the cover lower bound
 */
public int getUB(int period, int activity)
{ return coverUB[period][activity]; }

/**
 * initialize the matrices and label.
 * @param coverMaxValue the maximum value for the cover (nb of employees)
 * @param label         the label of the cover
 * @param nbPeriods     the number of periods
 * @param nbActivities  the number of activities
 */
public Cover(int coverMaxValue, String label, int nbPeriods, int nbActivities)
{
	this.coverMaxValue = coverMaxValue;
	coverLB = new int[nbPeriods][nbActivities];
	coverUB = new int[nbPeriods][nbActivities];
	coverSoftLB = new int[nbPeriods][nbActivities];
	coverSoftUB = new int[nbPeriods][nbActivities];
	for (int t = 0; t < nbPeriods; t++) {
		for (int a = 0; a < nbActivities; a++) {
			coverUB[t][a] = coverMaxValue;
			coverSoftUB[t][a] = coverMaxValue;
		}
	}
	this.label = label;
}

/**
 * initialize the values of the bounds for a given activity.
 * @param periodIndexes the list of the covered periods
 * @param activity      the activity
 * @param min           the lower bound
 * @param max           the upper bound
 * @param prefMin       the soft lower bound
 * @param prefMax       the soft upper bound
 */
public void setValues(int[] periodIndexes, int activity, Integer min, Integer max, Integer prefMin, Integer prefMax)
{
	for (int period : periodIndexes) { setValues(period, activity, min, max, prefMin, prefMax); }
}

/**
 * initialize the values of the bounds for a given activity.
 * @param period   the covered period
 * @param activity the activity
 * @param min      the lower bound
 * @param max      the upper bound
 * @param prefMin  the soft lower bound
 * @param prefMax  the soft upper bound
 */
public void setValues(int period, int activity, Integer min, Integer max, Integer prefMin, Integer prefMax)
{
	coverLB[period][activity] = (min == null) ? ((prefMin == null) ? 0 : prefMin) : min;
	coverUB[period][activity] = (max == null) ? ((prefMax == null) ? coverMaxValue : prefMax) : max;
	coverSoftLB[period][activity] = (prefMin == null) ? coverLB[period][activity] : prefMin;
	coverSoftUB[period][activity] = (prefMax == null) ? coverUB[period][activity] : prefMax;
}

/**
 * get the cover lower bounds of all activities for a period
 * @param period the period
 * @return the cover lower bounds [nbActs]
 */
public int[] getLB(int period)
{ return coverLB[period]; }

/**
 * get the cover upper bounds of all activities for a period
 * @param period the period
 * @return the cover lower bounds [nbActs]
 */
public int[] getUB(int period)
{ return coverUB[period]; }

}
