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
 * Date: Mar 1, 2010 - 9:02:08 AM
 */

package etp.data.asap3;

import asap3.*;
import choco.automaton.FA.FiniteAutomaton;
import choco.kernel.common.logging.ChocoLogging;
import etp.data.DataBuilder;
import etp.data.components.*;
import etp.data.components.Cover;
import gnu.trove.TIntArrayList;
import org.apache.xmlbeans.XmlException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/** @author Sophie Demassey */
public class ASAP3DataBuilder extends DataBuilder {

private SchedulingPeriodDocument.SchedulingPeriod root;
private Map<String, FiniteAutomaton> contractAutomata;
Map<String, SkillCover> skillCovers;
etp.data.components.Cover globalCover;
Logger LOGGER = ChocoLogging.getMainLogger();

/**
 * intialize data from an ASAP instance
 * @param instanceFile the ASAP instance file name
 */
public ASAP3DataBuilder(String instanceFile, Properties properties)
{
	super(properties);
	SchedulingPeriodDocument document = null;
	try {
		document = SchedulingPeriodDocument.Factory.parse(new File(instanceFile));
	} catch (IOException e) {
		e.printStackTrace();
	} catch (XmlException e) {
		e.printStackTrace();
	}
	if (document == null) throw new IllegalArgumentException("Unable to parse XML file " + document);
	this.root = document.getSchedulingPeriod();
}

@Override
protected String readProblemName() { return root.getID(); }

/**
 * intialize data from an NRP10 instance.
 * Read the XML elements: StartDate, EndDate, ShiftTypes
 */
@Override
protected Activities buildActivities()
{
	ShiftTypes.Shift[] shifts = root.getShiftTypes().getShiftArray();
	Map<String, Integer> actIndex = new HashMap<String, Integer>();
	int index = 0;
	for (ShiftTypes.Shift shift : shifts) {
		actIndex.put(shift.getID(), index++);
	}
	assert index == actIndex.size();
	Activities activities = new Activities(actIndex, null);
	this.addShiftGroups(activities);
	return activities;
}

private void addShiftGroups(Activities activities)
{
	if (!root.isSetShiftGroups()) return;
	activities.initShiftGroups();
	for (ShiftGroups.ShiftGroup g : root.getShiftGroups().getShiftGroupArray()) {
		int[] act = new int[g.sizeOfShiftArray()];
		int i = 0;
		for (String a : g.getShiftArray()) { act[i++] = activities.getActivityIndex(a); }
		activities.addShiftGroup(g.getID(), act);
	}
}


/**
 * intialize data from an NRP10 instance.
 * Read the XML elements: StartDate, EndDate, ShiftTypes
 */
@Override
protected Periods buildPeriods()
{
	Calendar startDay = root.getStartDate();
	Calendar endDay = root.getEndDate();

	return new Periods(startDay, endDay);
}

@Override
protected Employee[] buildEmployees()
{
	Map<String, Employee> employeeMap = new HashMap<String, Employee>();
	List<String> sortedKeys = new ArrayList<String>();
	for (Employees.Employee e : root.getEmployees().getEmployeeArray()) {
		String contractId = e.getContractID();
		String name = (e.getName() != null) ? e.getName() : contractId + "_" + employeeMap.size();
		employeeMap.put(e.getID(), new Employee(e.getID(), name, contractId, data.getPeriods().getNbDays(), data.getActivities().getNbActivities(), properties.hardSoftLimit));
		sortedKeys.add(e.getID());
		if (e.isSetInRoster() && !e.getInRoster()) {
			LOGGER.warning(data.getProblemName() + " WARNING not parsed element: Employee/InRoster");
		}

	}
	this.parseRequestAssignmentCosts(employeeMap, data.getPeriods(), data.getActivities());


	Map<String, Integer> employeeIndexMap = new HashMap<String, Integer>();
	Employee[] sortedEmployees = new Employee[sortedKeys.size()];
	int e = 0;
	for (String id : sortedKeys) {
		sortedEmployees[e] = employeeMap.get(id);
		employeeIndexMap.put(id, e);
		e++;
	}

	this.parseSkills(employeeIndexMap);


	return sortedEmployees;

}

/** intialize the skills from an ASAP3 instance. */
protected void parseSkills(Map<String, Integer> employeeIndexMap)
{
	skillCovers = new HashMap<String, SkillCover>();

	if (root.isSetSkills()) {
		for (Skills.Skill s : root.getSkills().getSkillArray()) {
			TIntArrayList emplist = new TIntArrayList();
			for (Employees.Employee e : root.getEmployees().getEmployeeArray()) {
				if (e.isSetSkills()) {
					for (String sk : e.getSkills().getSkillArray()) {
						if (sk.equals(s.getID())) {
							emplist.add(employeeIndexMap.get(e.getID()));
							break;
						}
					}
				}
			}
			skillCovers.put(s.getID(), new SkillCover(s.getID(), emplist.toNativeArray(), data.getPeriods().getNbDays(), data.getActivities().getNbActivities()));
		}
	}

	if (root.isSetSkillGroups()) {
		for (SkillGroups.SkillGroup g : root.getSkillGroups().getSkillGroupArray()) {
			TIntArrayList emplist = new TIntArrayList();
			int nb = 0;
			for (String s : g.getSkillArray()) {
				SkillCover skill = skillCovers.get(s);
				emplist.add(skill.getEmployees());
				nb += skill.getEmployees().length;
			}
			skillCovers.put(g.getID(), new SkillCover(g.getID(), emplist.toNativeArray(), data.getPeriods().getNbDays(), data.getActivities().getNbActivities()));
		}
	}
}


private void parseRequestAssignmentCosts(Map<String, Employee> employees, Periods periods, Activities activities)
//private void addAssignmentCosts()
{
	if (root.isSetDayOffRequests()) {
		for (DayOffRequests.DayOff req : root.getDayOffRequests().getDayOffArray()) {
			employees.get(req.getEmployeeID()).setMandatory(periods.getPeriodIndex(req.getDate(), req.getDay()), activities.getRestIndex(), req.getWeight().intValue());
		}
	}
	if (root.isSetDayOnRequests()) {
		for (DayOnRequests.DayOn req : root.getDayOnRequests().getDayOnArray()) {
			employees.get(req.getEmployeeID()).setForbidden(periods.getPeriodIndex(req.getDate(), req.getDay()), activities.getRestIndex(), req.getWeight().intValue());
		}
	}

	if (root.isSetShiftOffRequests()) {
		for (ShiftOffRequests.ShiftOff req : root.getShiftOffRequests().getShiftOffArray()) {
			employees.get(req.getEmployeeID()).setForbidden(periods.getPeriodIndex(req.getDate(), req.getDay()), activities.getActivityIndex(req.getShiftTypeID()), req.getWeight().intValue());
		}
	}

	if (root.isSetShiftOnRequests()) {
		for (ShiftOnRequests.ShiftOn req : root.getShiftOnRequests().getShiftOnArray()) {
			Employee employee = employees.get(req.getEmployeeID());
			int period = periods.getPeriodIndex(req.getDate(), req.getDay());
			int weight = req.getWeight().intValue();
			if (req.isSetShiftTypeID()) {
				employee.setMandatory(period, activities.getActivityIndex(req.getShiftTypeID()), weight);
			} else if (req.isSetShiftGroupID()) {
				employee.setMandatory(period, activities.getShiftGroup(req.getShiftGroupID()), weight);
			} else if (req.isSetShiftGroup()) {
				TIntArrayList shifts = new TIntArrayList();
				for (String shiftId : req.getShiftGroup().getShiftArray()) {
					shifts.add(activities.getActivityIndex(shiftId));
				}
				shifts.sort();
				employee.setMandatory(period, shifts.toNativeArray(), weight);
			} else {
				LOGGER.warning("error while parsing ShiftOnRequest " + req);
			}
		}
	}
	if (root.isSetFixedAssignments()) {
		for (FixedAssignments.Employee e : root.getFixedAssignments().getEmployeeArray()) {
			Employee employee = employees.get(e.getEmployeeID());
			for (FixedAssignments.Employee.Assign req : e.getAssignArray()) {
				int period = periods.getPeriodIndex(req.getDate(), req.getDay());
				String s = req.getShift();
				int activity = (s.equals("-")) ? activities.getRestIndex() : activities.getActivityIndex(s);
				employee.setMandatory(period, activity);
			}
		}
	}

	for (ShiftTypes.Shift shift : root.getShiftTypes().getShiftArray()) {
		if (shift.isSetAutoAllocate() && !shift.getAutoAllocate()) {
			int activity = activities.getActivityIndex(shift.getID());
			for (Employee employee : data.getEmployees()) { employee.setForbidden(activity); }
			LOGGER.finer("Remove fixed assignment shifts" + activity);
		}
	}
}


private void parseCovers()
{
	LOGGER.finer("cover parser................................................................................");
	LOGGER.warning("WARNING CoverConstraints : no violation costs are associated to covers.");

	for (CoverRequirements.DateSpecificCover req : root.getCoverRequirements().getDateSpecificCoverArray()) {
		int[] dayIndexes = new int[]{data.getPeriods().getDayIndex(req.getDate())};
		LOGGER.finer(req.sizeOfCoverArray() + " covers " + dayIndexes[0] + ": ");
		for (asap3.Cover cov : req.getCoverArray()) { this.parseCover(cov, dayIndexes); }
	}

	for (CoverRequirements.DayOfWeekCover req : root.getCoverRequirements().getDayOfWeekCoverArray()) {
		int[] dayIndexes = data.getPeriods().getAllIndexesForDayOfWeek(req.getDay());
		LOGGER.finer(req.sizeOfCoverArray() + " covers " + req.getDay() + ": ");
		for (asap3.Cover xmlCover : req.getCoverArray()) { this.parseCover(xmlCover, dayIndexes); }
	}
}

private void parseCover(asap3.Cover xmlCover, int[] dayIndexes)
{
	if (xmlCover.isSetSkill()) {
		this.addCoverValue(skillCovers.get(xmlCover.getSkill()), xmlCover, dayIndexes);
	} else if (xmlCover.isSetSkillGroup()) {
		this.addCoverValue(skillCovers.get(xmlCover.getSkillGroup()), xmlCover, dayIndexes);
	} else {
		this.addCoverValue(globalCover, xmlCover, dayIndexes);
	}
}

/**
 * add a cover value for a given activity and time period.
 * Read the XML elements: Cover
 * @param cover      the Cover object to parse
 * @param dayIndexes the list of days associated to the cover requirement
 */
private void addCoverValue(Cover cover, asap3.Cover xmlCover, int[] dayIndexes)
{
	if (xmlCover.isSetTimePeriod()) {
		LOGGER.warning(data.getProblemName() + " WARNING not parsed element: Cover/TimePeriod");
	}
	if (!xmlCover.isSetShift()) throw new RuntimeException(data.getProblemName() + ": not implemented in addCover.");
	int activity = data.getActivities().getActivityIndex(xmlCover.getShift());
	Integer min = (xmlCover.isSetMin()) ? xmlCover.getMin().intValue() : null;
	Integer max = (xmlCover.isSetMax()) ? xmlCover.getMax().intValue() : null;
	Integer pref = (xmlCover.isSetPreferred()) ? xmlCover.getPreferred().intValue() : null;
	cover.setValues(dayIndexes, activity, min, max, pref, pref);
	LOGGER.finer(cover.getLabel() + " " + activity + " [" + min + "," + pref + "," + max + "] ");
}


/**
 * intialize data from an NRP10 instance.
 * Read the XML elements: CoverRequirements
 */
@Override
protected Cover buildGlobalCover()
{
	if (globalCover == null) {
		globalCover = new Cover(data.getNbEmployees(), "all", data.getPeriods().getNbDays(), data.getActivities().getNbActivities());
		this.parseCovers();
	}
	return globalCover;
}

/**
 * intialize data from an NRP10 instance.
 * There are no specific skill cover (but there are specific skill shifts)
 * Read the XML elements: CoverRequirements
 */
@Override
protected Map<String, SkillCover> buildSkillCovers()
{
	assert globalCover != null && skillCovers != null : "skill covers not created";
	return skillCovers;
}


@Override
protected void buildAutomata()
{
	contractAutomata = new HashMap<String, FiniteAutomaton>();
	ASAP3AutomatonBuilder builder = new ASAP3AutomatonBuilder(data.getPeriods(), data.getActivities(), properties.hardSoftLimit);
	for (Contracts.Contract contract : root.getContracts().getContractArray()) {
		builder.setContract(contract);
		contractAutomata.put(contract.getID(), builder.buildAutomata().get("mca"));
	}
}

@Override
protected FiniteAutomaton getAutomaton(Employee e)
{
	return contractAutomata.get(e.getContractId());
}

}