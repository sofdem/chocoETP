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

package etp.solver.lns;

import choco.kernel.solver.Configuration;

import java.lang.reflect.Field;

import static choco.kernel.common.util.tools.PropertyUtils.logOnFailure;

/**
 * additional settings for Large Neighborhood Search based on CP
 * @author Sophie Demassey
 * @see LNSCPSolver
 */
public class LNSCPConfiguration extends Configuration {

/**
 * the limit type set on the B&B in the initial step of LNS
 * @see choco.kernel.solver.search.limit.Limit
 */
@Default(value = "BACKTRACK")
public static final String LNS_INIT_SEARCH_LIMIT = "lns.initial.cp.search.limit.type";

/** the limit value set on the B&B in the initial step of LNS */
@Default(value = "1000")
public static final String LNS_INIT_SEARCH_LIMIT_BOUND = "lns.initial.cp.search.limit.value";

/**
 * the limit type set on the backtracking in each neighborhood exploration of LNS
 * @see choco.kernel.solver.search.limit.Limit
 */
@Default(value = "BACKTRACK")
public static final String LNS_NEIGHBORHOOD_SEARCH_LIMIT = "lns.neighborhood.cp.search.limit.type";

/** the limit value set on the backtracking in each neighborhood exploration of LNS */
@Default(value = "1000")
public static final String LNS_NEIGHBORHOOD_SEARCH_LIMIT_BOUND = "lns.neighborhood.cp.search.limit.value";

/** the number of iterations of the loop in the second step of LNS */
@Default(value = "3")
public static final String LNS_RUN_LIMIT_NUMBER = "lns.run.limit.number";

/** a boolean indicating wether the CP model must be solved by LNS or B&B */
@Default(value = "true")
public static final String LNS_USE = "lns.use";

public LNSCPConfiguration()
{
	super();
}

/**
 * Load the default value of keys defined in @Default annotation
 * @param key the name of the field
 */
public String loadDefault(String key)
{
	Field[] fields = LNSCPConfiguration.class.getFields();
	for (Field f : fields) {
		try {
			if (f.get(this).equals(key)) {
				Default ann = f.getAnnotation(Default.class);
				return ann.value();
			}
		} catch (IllegalAccessException e) {
			logOnFailure(key);
		}
	}
	throw new NullPointerException("cant find ");
}

}
