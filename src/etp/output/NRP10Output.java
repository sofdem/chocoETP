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

package etp.output;/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 24/02/11 - 12:53
 */

import etp.data.components.Activities;
import etp.data.components.Data;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/** @author Sophie Demassey */
public class NRP10Output implements ETPOutput {

String headerXml;
Calendar startDay;
DateFormat dateFormat;
String[] employeeIds;
Activities activities;
String filename;

public NRP10Output(Data data, String outputFileName)
{
	this(data.getProblemName(), data.getPeriods().getStartDay(), data.getEmployeeIds(), data.getActivities(), outputFileName);
}

private NRP10Output(String problemName, Calendar startDay, String[] employeeNames, Activities activities, String fileName)
{
	StringBuffer s = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?><?cocoon-format type=\"text/xml\"?>\n");
	s.append("<Solution xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"solution.xsd\">");
	s.append("<SchedulingPeriodID>").append(problemName).append("</SchedulingPeriodID>\n");
	s.append("<Competitor>Menana, Demassey / EMNantes</Competitor>\n");
	this.headerXml = s.toString();
	this.startDay = startDay;
	this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	this.employeeIds = employeeNames;
	this.activities = activities;
	this.filename = fileName;


}

/**
 * write the nrp10_long.timetabling solution in the NRP XML format.
 * @return the output as a string
 */
private String solutionToXML(int objectiveValue, int shiftValues[][])
{
	StringBuffer s = new StringBuffer(this.headerXml);
	s.append("<SoftConstraintsPenalty>").append(objectiveValue).append("</SoftConstraintsPenalty>\n");
	for (int e = 0; e < shiftValues.length; e++) {
		String sub = "</Date><Employee>" + employeeIds[e] + "</Employee><ShiftType>";
		Calendar date = (Calendar) startDay.clone();
		for (int val : shiftValues[e]) {
			if (activities.isNotRestActivity(val)) {
				s.append("<Assignment>\n<Date>").append(dateFormat.format(date.getTime())).append(sub).append(activities.getActivityLabel(val)).append("</ShiftType></Assignment>\n");
			}
			date.add(Calendar.DAY_OF_MONTH, 1);
		}
	}
	s.append("</Solution>\n");
	return s.toString();
}

public void writeSolution(int objectiveValue, int shiftValues[][])
{
	this.writeXMLSolution(objectiveValue, shiftValues);
}

/** write the nrp10_long.timetabling solution in the NRP XML format in a file. */
void writeXMLSolution(int objectiveValue, int shiftValues[][])
{
	BufferedWriter writer = null;
	try {
		String content = this.solutionToXML(objectiveValue, shiftValues);
		writer = new BufferedWriter(new FileWriter(filename));
		writer.write(content);
	} catch (FileNotFoundException ex) {
		ex.printStackTrace();
	} catch (IOException ex) {
		ex.printStackTrace();
	} finally {
		try {
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		} catch (IOException ex) { ex.printStackTrace(); }
	}
}


}
