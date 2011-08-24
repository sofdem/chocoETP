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

package etp.model;


import etp.data.components.Cover;
import etp.data.components.Data;
import etp.data.components.SkillCover;

import java.util.Map;
import java.util.Properties;

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: Nov 15, 2010 - 12:10:02 PM
 */

/** @author Sophie Demassey */
public class TimetableModelBuilder extends EtpModelBuilder {

public TimetableModelBuilder(Data data, Properties properties)
{
	super(data, properties);
}

public void buildModel()
{
	model = new TimetableModel(data.getPeriods().getNbDays(), data.getActivities().getNbActivities(), data.getNbEmployees());
	this.addRequestAssignments();
	this.addGlobalCover();
	this.addSkillCovers();
	this.addEmployeeRules();
	this.addLinkingConstraints();
	this.addObjective();
}

protected void addRequestAssignments()
{
	for (int e = 0; e < data.getNbEmployees(); e++) {
		data.setEmployeeMaxAssignmentCost(e, properties.objectiveUB);
		for (int t = 0; t < data.getPeriods().getNbDays(); t++) {
			for (int a = 0; a < data.getActivities().getNbActivities(); a++) {
				if (data.isForbidden(e, t, a)) {
					model.postForbiddenAssignment(e, t, a);
				}
			}
		}
	}
}

protected void addGlobalCover()
{
	Cover cover = data.getGlobalCover();
	model.postGlobalCover(cover, properties.coverGlobal);
}

protected void addSkillCovers()
{
	Map<String, SkillCover> skills = data.getSkillCovers();
	for (SkillCover skill : skills.values()) {
		model.postPartialCover(skill.getEmployees(), skill, properties.coverSkills);
	}
}

protected void addEmployeeRules()
{
	for (int e = 0; e < data.getNbEmployees(); e++) {
		model.postRules(e, data.getCostAutomaton(e, properties.aggregateCounters, properties.aggregateCountersWithRedundancy), properties.employeeRules);
	}
}

protected void addObjective()
{
	model.postObjective(0, properties.objectiveUB, properties.objective);
}

protected void addLinkingConstraints()
{
	String[] options = properties.linkingConstraints();
	if (options.length > 0) {
		for (String option : options) {
			model.postLinkingConstraints(option);
		}
	}
}


}
