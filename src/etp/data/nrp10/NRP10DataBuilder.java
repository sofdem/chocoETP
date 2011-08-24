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
 * Date: Mar 16, 2010 - 2:09:08 PM
 */

package etp.data.nrp10;

import choco.automaton.FA.FiniteAutomaton;
import etp.data.DataBuilder;
import etp.data.components.*;
import etp.data.components.Cover;
import nrp.competition.*;
import org.apache.xmlbeans.XmlException;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** @author Sophie Demassey */
public class NRP10DataBuilder extends DataBuilder {

private SchedulingPeriodDocument.SchedulingPeriod root;
private Map<String, FiniteAutomaton> contractAutomata;

/**
 * initialize the Data Builder for a NRP10 instance
 * @param instanceFile the NRP10 instance file name
 */
public NRP10DataBuilder(String instanceFile, Properties properties)
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
	return new Activities(actIndex, null);
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


/**
 * intialize data from an NRP10 instance.
 * Read the XML elements: Employees
 */
protected Employee[] buildEmployees()
{
	Map<String, Employee> employees = new HashMap<String, Employee>();
	List<String> sortedKeys = new ArrayList<String>();
	for (Employees.Employee e : root.getEmployees().getEmployeeArray()) {
		String contractId = e.getContractID();
		//String[] skills = e.getSkills().getSkillArray();
		String name = (e.getName() != null) ? e.getName() : contractId + "_" + employees.size();
		employees.put(e.getID(), new Employee(e.getID(), name, contractId, data.getPeriods().getNbDays(), data.getActivities().getNbActivities(), properties.hardSoftLimit));
		sortedKeys.add(e.getID());
	}
	this.parseRequestAssignmentCosts(employees, data.getPeriods(), data.getActivities());
	this.parseSkillAssignmentCosts(employees, data.getActivities());

	Employee[] sortedEmployees = new Employee[sortedKeys.size()];
	int e = 0;
	for (String id : sortedKeys) {
		sortedEmployees[e++] = employees.get(id);
	}
	return sortedEmployees;
}

/**
 * intialize data from an NRP10 instance.
 * Read the XML elements: DayOffRequests, DayOnRequests, ShiftOffRequests, ShiftOnRequests
 */
private void parseRequestAssignmentCosts(Map<String, Employee> employees, Periods periods, Activities activities)
{
	if (root.isSetDayOffRequests()) {
		for (DayOffRequests.DayOff req : root.getDayOffRequests().getDayOffArray()) {
			employees.get(req.getEmployeeID()).setMandatory(periods.getDayIndex(req.getDate()), activities.getRestIndex(), req.getWeight().intValue());
		}
	}
	if (root.isSetDayOnRequests()) {
		for (DayOnRequests.DayOn req : root.getDayOnRequests().getDayOnArray()) {
			employees.get(req.getEmployeeID()).setForbidden(periods.getDayIndex(req.getDate()), activities.getRestIndex(), req.getWeight().intValue());
		}
	}

	if (root.isSetShiftOffRequests()) {
		for (ShiftOffRequests.ShiftOff req : root.getShiftOffRequests().getShiftOffArray()) {
			employees.get(req.getEmployeeID()).setForbidden(periods.getDayIndex(req.getDate()), activities.getActivityIndex(req.getShiftTypeID()), req.getWeight().intValue());
		}
	}

	if (root.isSetShiftOnRequests()) {
		for (ShiftOnRequests.ShiftOn req : root.getShiftOnRequests().getShiftOnArray()) {
			employees.get(req.getEmployeeID()).setMandatory(periods.getDayIndex(req.getDate()), activities.getActivityIndex(req.getShiftTypeID()), req.getWeight().intValue());
		}
	}
}

private void parseSkillAssignmentCosts(Map<String, Employee> employees, Activities activities)
{
	if (!root.isSetSkills() || root.getSkills().getSkillArray().length == 0) return;

	Map<String, Integer> notSkillWeights = new HashMap<String, Integer>();
	for (Contracts.Contract contract : root.getContracts().getContractArray()) {
		Integer weight = (contract.getAlternativeSkillCategory().getBooleanValue()) ? contract.getAlternativeSkillCategory().getWeight().intValue() : 0;
		assert weight >= 0 : "skill assignment costs must be non-negative";
		notSkillWeights.put(contract.getID(), weight);
	}

	for (ShiftTypes.Shift shift : root.getShiftTypes().getShiftArray()) {
		int activity = activities.getActivityIndex(shift.getID());
		if (shift.isSetSkills() && shift.getSkills().sizeOfSkillArray() > 0) {
			for (Employees.Employee e : root.getEmployees().getEmployeeArray()) {
				if (e.isSetSkills()) {
					boolean hasSkill = false;
					for (String empSkill : e.getSkills().getSkillArray()) {
						for (String actSkill : shift.getSkills().getSkillArray()) {
							if (empSkill.equals(actSkill)) {
								hasSkill = true;
								break;
							}
							if (hasSkill) break;
						}
					}
					if (!hasSkill) {
						Employee emp = employees.get(e.getID());
						emp.setForbiddenWeight(activity, notSkillWeights.get(e.getContractID()));
					}
				}
			}
		}
	}
}


/**
 * intialize data from an NRP10 instance.
 * Read the XML elements: CoverRequirements
 */
protected Cover buildGlobalCover()
{
	Cover globalCover = new Cover(data.getNbEmployees(), "all", data.getPeriods().getNbDays(), data.getActivities().getNbActivities());
	for (CoverRequirements.DateSpecificCover req : root.getCoverRequirements().getDateSpecificCoverArray()) {
		int[] dayIndexes = new int[]{data.getPeriods().getDayIndex(req.getDate())};
		for (nrp.competition.Cover cov : req.getCoverArray()) { this.addCover(globalCover, cov, dayIndexes); }
	}

	for (CoverRequirements.DayOfWeekCover req : root.getCoverRequirements().getDayOfWeekCoverArray()) {
		int[] dayIndexes = data.getPeriods().getAllIndexesForDayOfWeek(req.getDay().toString());
		for (nrp.competition.Cover cov : req.getCoverArray()) { this.addCover(globalCover, cov, dayIndexes); }
	}
	return globalCover;
}

/**
 * add a cover value for a given activity and time period.
 * Read the XML elements: Cover
 * @param cover      the Cover object to parse
 * @param dayIndexes the list of days associated to the cover requirement
 */
private void addCover(Cover globalCover, nrp.competition.Cover cover, int[] dayIndexes)
{
	int activity = data.getActivities().getActivityIndex(cover.getShift());
	assert cover.isSetPreferred();
	Integer pref = cover.getPreferred().intValue();
	globalCover.setValues(dayIndexes, activity, null, null, pref, pref);
}

/**
 * intialize data from an NRP10 instance.
 * There are no specific skill cover (but there are specific skill shifts)
 * Read the XML elements: CoverRequirements
 */
protected Map<String, SkillCover> buildSkillCovers()
{
	Map<String, SkillCover> covers = new HashMap<String, SkillCover>(0);
	return covers;
}


protected void buildAutomata()
{
	NRP10AutomatonBuilder builder = new NRP10AutomatonBuilder(data.getPeriods(), data.getActivities(), root.getPatterns());
	contractAutomata = new HashMap<String, FiniteAutomaton>();
	for (Contracts.Contract contract : root.getContracts().getContractArray()) {
		builder.setContract(contract);
//		Map<String, FiniteAutomaton> contractAutomata = builder.buildAutomata(properties.useGlobalAssignmentCounter, properties.useIndividualAssignmentCounter);
		contractAutomata.put(contract.getID(), builder.buildAutomata().get("mca"));
	}
}

protected FiniteAutomaton getAutomaton(Employee e)
{
	return contractAutomata.get(e.getContractId());
}

}
