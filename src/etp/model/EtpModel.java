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

import choco.automaton.FA.FiniteAutomaton;
import choco.kernel.model.Model;
import etp.data.components.Cover;

/*
 * Created by IntelliJ IDEA.
 * User: sofdem - sophie.demassey{at}mines-nantes.fr
 * Date: Nov 15, 2010 - 11:42:40 AM
 */

/** @author Sophie Demassey */
public interface EtpModel extends Model {

/** initialize the model and its core shift variables */
public void postForbiddenAssignment(int e, int t, int a);

public void postMandatoryAssignment(int e, int t, int a);

public void postGlobalCover(Cover cover, String option);

public void postPartialCover(int[] employees, Cover cover, String option);

public void postObjective(int lb, int ub, String option);

public void postRules(int e, FiniteAutomaton automaton, String option);

public void postLinkingConstraints(String option);


}
