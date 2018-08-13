/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.cohort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.reporting.PatientSearch;
import org.openmrs.reporting.ReportObjectService;

public class CohortUtil {

	private static final String COHORT_CHECK_NULLPATIENTS_GLOBAL_PROPERTY_NAME = "cohort.checknullpatients";
	/**
	 * Parses an input string like: [Male] and [Adult] and
	 * [EnrolledInHivOnDate|program="1"|untilDate="${report.startDate}"] Names between brackets are
	 * treated as saved PatientSearch objects with that name. Parameter values for those loaded
	 * searches are specified after a | The following are handled like they would be in a cohort
	 * builder composition search: ( ) and or not
	 *
	 * @param spec
	 * @return A CohortDefinition (currently always a PatientSearch) parsed from the spec string.
	 * @should parse specification with and in it
	 */
	public static CohortDefinition parse(String spec) {
		List<Object> tokens = new ArrayList<Object>();
		{
			StringBuilder thisElement = null;
			for (int i = 0; i < spec.length(); ++i) {
				char c = spec.charAt(i);
				switch (c) {
					case '(':
					case ')':
						if (thisElement != null) {
							tokens.add(thisElement.toString().trim());
							thisElement = null;
						}
						tokens.add("" + c);
						break;
					case ' ':
					case '\t':
					case '\n':
						if (thisElement != null) {
							thisElement.append(c);
						}
						break;
					case '[':
						if (thisElement != null) {
							tokens.add(thisElement.toString().trim());
						}
						thisElement = new StringBuilder();
						thisElement.append(c);
						break;
					default:
						if (thisElement == null) {
							thisElement = new StringBuilder();
						}
						thisElement.append(c);
						if (c == ']') {
							tokens.add(thisElement.toString().trim());
							thisElement = null;
						}
						break;
				}
			}
			if (thisElement != null) {
				tokens.add(thisElement.toString().trim());
			}
		}
		for (ListIterator<Object> i = tokens.listIterator(); i.hasNext();) {
			Object o = i.next();
			if (o instanceof String) {
				String s = (String) o;
				if (s.startsWith("[") && s.endsWith("]")) {
					s = s.substring(1, s.length() - 1);
					String name = null;
					Map<String, String> paramValues = new HashMap<String, String>();
					StringTokenizer st = new StringTokenizer(s, "|");
					while (st.hasMoreTokens()) {
						String t = st.nextToken();
						if (name == null) {
							name = t;
						} else {
							int ind = t.indexOf('=');
							if (ind < 0) {
								throw new IllegalArgumentException("The fragment '" + t + "' in " + s + " has no =");
							}
							paramValues.put(t.substring(0, ind), t.substring(ind + 1));
						}
					}
					if (name == null) {
						throw new IllegalArgumentException("Could not find a cohort name in " + s);
					}
					PatientSearch search = Context.getService(ReportObjectService.class).getPatientSearch(name);
					if (search == null) {
						throw new IllegalArgumentException("Could not load a cohort named " + name);
					}
					for (Map.Entry<String, String> e : paramValues.entrySet()) {
						search.setParameterValue(e.getKey(), e.getValue());
					}
					i.set(search);
				}
			}
		}
		
		return PatientSearch.createCompositionSearch(tokens);
	}

	/**
	 * This method will be used to check for the patient object by the given patientId to confirm the patient is not null.
	 * Some modules (eg : location based access control) may block the access of the certain patient objects when accessing from
	 * other locations.
	 * @param patientId patientId
	 * @return the patient is exist for the access or not
	 */
	public static Boolean doesPatientExist(Integer patientId) {
		if (!doesNeedToCheckForNullPatients())
			return true;
		PatientService patientService = Context.getPatientService();
		if(patientId != null && patientService.getPatient(patientId) != null) {
			return true;
		}
		return false;
	}

	/**
	 * This method will be used to check for the patients object by the given patientIds to confirm the patients are not null.
	 * Some modules (eg : location based access control) may block the access of the certain patient objects when accessing from
	 * other locations.
	 * @param patientIds Set of all patientIds
	 * @return Set of patientIds with only accessible patients
	 */
	public static Set<Integer> removeNullPatients(Set<Integer> patientIds) {
		return (Set<Integer>)removeNullPatients((Collection<Integer>)patientIds);
	}

	/**
	 * This method will be used to check for the patients object by the given patientIds to confirm the patients are not null.
	 * Some modules (eg : location based access control) may block the access of the certain patient objects when accessing from
	 * other locations.
	 * @param patientIds Collection of all patientIds
	 * @return Collection of patientIds with only accessible patients
	 */
	public static Collection<Integer> removeNullPatients(Collection<Integer> patientIds) {
		if(patientIds == null || !doesNeedToCheckForNullPatients()) {
			return patientIds;
		}
		PatientService patientService = Context.getPatientService();
		Iterator<Integer> iterator = patientIds.iterator();
		while (iterator.hasNext()) {
			Integer patientId = iterator.next();
			if (patientService.getPatient(patientId) == null) {
				iterator.remove();
			}
		}
		return patientIds;
	}

	private static Boolean doesNeedToCheckForNullPatients() {
		String gpValue = Context.getAdministrationService().getGlobalProperty("COHORT_CHECK_NULLPATIENTS_GLOBAL_PROPERTY_NAME");
		return (gpValue != null && gpValue.toLowerCase().equals("true"));
	}
}
