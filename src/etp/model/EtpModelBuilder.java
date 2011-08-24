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
/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: Nov 15, 2010 - 11:45:51 AM
 */

import etp.data.components.Data;

import java.util.Properties;

/** @author Sophie Demassey */
public abstract class EtpModelBuilder {

protected final Data data;
protected final ModelProperties properties;
protected EtpModel model;

protected class ModelProperties {
	int objectiveUB;
	String coverGlobal;
	String coverSkills;
	String employeeRules;
	boolean aggregateCounters;
	String objective;
	boolean aggregateCountersWithRedundancy;
	private String linkingConstraints;

	public ModelProperties(Properties properties)
	{
		this.objectiveUB = Integer.parseInt(properties.getProperty("model.objective.ub"));
		this.coverGlobal = properties.getProperty("model.cover.global");
		this.coverSkills = properties.getProperty("model.cover.skills");
		this.employeeRules = properties.getProperty("model.employee.rules");
		this.aggregateCounters = Boolean.parseBoolean(properties.getProperty("model.employee.rules.counters.aggregate"));
		this.aggregateCountersWithRedundancy = Boolean.parseBoolean(properties.getProperty("model.employee.rules.counters.aggregate.keepRedundant"));
		this.objective = properties.getProperty("model.objective");
		this.linkingConstraints = "";
		if (Boolean.parseBoolean(properties.getProperty("model.linking.cover.counters.global.shifts"))) {
			this.linkingConstraints += "GS ";
		}
	}

	@Override
	public String toString()
	{
		return objectiveUB + ": " + coverGlobal + " " + coverSkills + " " + employeeRules + " " + linkingConstraints + " " + objective;
	}

	public String[] linkingConstraints() { return linkingConstraints.split(" "); }

}

public EtpModelBuilder(Data data, Properties properties)
{
	this.data = data;
	this.properties = new ModelProperties(properties);
}


public EtpModel getModel() { return model; }

public abstract void buildModel();

}
