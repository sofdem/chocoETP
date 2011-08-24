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

package etp;

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: 28/01/11 - 16:58
 */

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.logging.Verbosity;
import etp.data.DataBuilder;
import etp.data.asap3.ASAP3DataBuilder;
import etp.data.components.Data;
import etp.data.nrp10.NRP10DataBuilder;
import etp.model.EtpModel;
import etp.model.EtpModelBuilder;
import etp.model.TimetableModelBuilder;
import etp.output.ETPOutput;
import etp.output.NRP10Output;
import etp.solver.EtpSolver;
import etp.solver.EtpSolverBuilder;
import etp.solver.TimetableCPSolverBuilder;
import org.eclipse.jdt.internal.jarinjarloader.JarRsrcLoader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

enum Bench {
	ASAP
			{
				String defaultPropertyFile() { return "data/timetabling.properties"; }

			},
	ASAP3
			{
				DataBuilder getDataBuilder(String instanceFile, Properties properties)
				{
					return new ASAP3DataBuilder(instanceFile, properties);
				}

				String defaultPropertyFile() { return directoryName() + "timetabling.properties"; }
			},
	NRP10
			{
				DataBuilder getDataBuilder(String instanceFile, Properties properties)
				{
					return new NRP10DataBuilder(instanceFile, properties);
				}

				ETPOutput getWriter(Data data, String outputFile) { return new NRP10Output(data, outputFile); }

				String defaultPropertyFile() { return directoryName() + "timetabling.properties"; }
			};

DataBuilder getDataBuilder(String instanceFile, Properties properties)
{
	throw new RuntimeException("no parser known for instance file " + instanceFile);
}

ETPOutput getWriter(Data data, String outputFile) { return null; }

String directoryName() { return "data/" + this.name() + "/"; }

String defaultPropertyFile() { return "data/timetabling.properties"; }

static Bench getBench(String instanceFile)
{
	for (Bench b : Bench.values()) { if (instanceFile.startsWith(b.directoryName())) return b; }
	return null;
}

}


/** @author Sophie Demassey */
public class ETPDirector {

private final Properties properties;
private final String instanceFile;
private final String solutionFile;
private final Bench bench;
private final long startTime;
private long nowTime;


public ETPDirector(String instanceFile, String propertiesFile)
{
	this.instanceFile = instanceFile;
	this.bench = Bench.getBench(instanceFile);
	if (bench == null) {
		throw new RuntimeException("Abort: unknown bench type " + bench);

	}
	this.properties = getProperties(propertiesFile);
	this.solutionFile = solutionFileName();
	this.startTime = System.currentTimeMillis();
	this.nowTime = System.currentTimeMillis();
}

private String elapsedTime()
{
	return (System.currentTimeMillis() - nowTime) + " ms";
}

private void recordTime()
{
	nowTime = System.currentTimeMillis();
}

private void setBuildersVerbosity()
{
	try {
		ChocoLogging.setVerbosity(Verbosity.valueOf(properties.getProperty("output.verbosity.builders")));
	} catch (Exception e) {
		e.printStackTrace();
	}
}

private void setSolverVerbosity()
{
	try {
		ChocoLogging.setVerbosity(Verbosity.valueOf(properties.getProperty("output.verbosity.solver")));
	} catch (Exception e) {
		e.printStackTrace();
	}
}

private Properties getProperties(String propertiesFile)
{
	String defaultPropFile = bench.defaultPropertyFile();
	Properties properties = new Properties();
	FileInputStream in;
	try {
		in = new FileInputStream(defaultPropFile);
		properties.load(in);
		in.close();
	} catch (IOException e) {
		e.printStackTrace();
		System.err.println("Abort: no default property file " + defaultPropFile + " found");
		System.exit(1);
	}
	if (!propertiesFile.isEmpty()) {
		try {
			in = new FileInputStream(propertiesFile);
			properties.load(in);
			in.close();
		} catch (IOException e) {
			System.err.println("property file " + propertiesFile + " not found: load default " + defaultPropFile);
		}
	}
	return properties;
}


private String solutionFileName()
{
	if (!Boolean.parseBoolean(properties.getProperty("output.evaluate"))) return null;

	int deb = instanceFile.lastIndexOf('/');
	String solFileName = properties.getProperty("output.evaluate.file.prefix") + instanceFile.substring(deb + 1);

	return (properties.getProperty("output.evaluate.directory") != null) ? properties.getProperty("output.evaluate.directory") + "/" + solFileName : solFileName;
}

public ETPOutput buildWriter(Data data)
{
	return (solutionFile == null) ? null : bench.getWriter(data, solutionFile);
}


public Data buildData()
{
	ChocoLogging.getMainLogger().info("build Data... ");
	this.recordTime();
	ChocoLogging.flushLogs();
	DataBuilder builder = bench.getDataBuilder(instanceFile, properties);
	builder.buildData();
	ChocoLogging.getMainLogger().info(this.elapsedTime());
	return builder.getData();
}

public EtpModel buildModel(Data data)
{
	ChocoLogging.getMainLogger().info("build Model... ");
	this.recordTime();
	ChocoLogging.flushLogs();
	EtpModelBuilder builder = new TimetableModelBuilder(data, properties);
	builder.buildModel();
	ChocoLogging.getMainLogger().info(this.elapsedTime());
	return builder.getModel();
}

public EtpSolver buildSolver(EtpModel model, ETPOutput writer)
{
	ChocoLogging.getMainLogger().info("build Solver... ");
	this.recordTime();
	ChocoLogging.flushLogs();
	EtpSolverBuilder builder = new TimetableCPSolverBuilder(properties);
	builder.buildSolver(model, writer);
	EtpSolver solver = builder.getSolver();
	if (ChocoLogging.getMainLogger().isLoggable(Level.FINER)) {
		ChocoLogging.getMainLogger().finer(solver.pretty());
	}
	ChocoLogging.getMainLogger().info(this.elapsedTime());
	return solver;
}

public void launch()
{
	this.setBuildersVerbosity();
	ChocoLogging.getMainLogger().info(instanceFile);
	Data data = this.buildData();
	EtpModel model = this.buildModel(data);
	ETPOutput writer = this.buildWriter(data);
	EtpSolver solver = this.buildSolver(model, writer);


	this.setSolverVerbosity();

	ChocoLogging.getMainLogger().info("start solving... ");
	this.recordTime();
	ChocoLogging.flushLogs();
	solver.solve();
	ChocoLogging.getMainLogger().info("solving elapsed time: " + this.elapsedTime());
	this.evaluate();
	ChocoLogging.getMainLogger().info("all elapsed time: " + (System.currentTimeMillis() - startTime) + " ms");
}

public void evaluate()
{
	if (bench != Bench.NRP10 || solutionFile == null) return;
	ChocoLogging.getMainLogger().info("evaluation... ");
	this.recordTime();
	ChocoLogging.flushLogs();
	try {
		JarRsrcLoader.main((this.instanceFile + " " + this.solutionFile).split(" "));
	} catch (Exception e) {
		e.printStackTrace();
	}
	ChocoLogging.getMainLogger().info("evaluation elapsed time: " + this.elapsedTime());
}


private static String readArgument(String[] args, String prefix, String suffixes)
{
	for (String suffix : suffixes.split("|")) {
		for (String arg : args) {
			if (arg.endsWith(suffix) && arg.startsWith(prefix)) {
				return arg;
			}
		}
	}
	return "";
}

public static void main(String[] args)
{
	String instanceFile = readArgument(args, "data/", ".xml|.ros");
	if (instanceFile.isEmpty()) {
		//instanceFile = "data/ASAP3/Ikegami-2Shift-DATA1.ros";
		//instanceFile = "data/ASAP3/Musa.ros";
		//instanceFile = "data/ASAP3/BCV-3.46.2.ros";
		//instanceFile = "data/ASAP3/GPost.ros";
		//instanceFile = "data/NRP10/medium/medium03.xml";
		// instanceFile = "data/NRP10/toy/toy2.xml";
		instanceFile = "data/NRP10/sprint/sprint02.xml";
	}
	String propertiesFile = readArgument(args, "", ".properties");

	ETPDirector etp = new ETPDirector(instanceFile, propertiesFile);
	etp.launch();
}


}
