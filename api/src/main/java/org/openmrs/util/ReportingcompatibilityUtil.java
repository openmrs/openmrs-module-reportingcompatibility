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
package org.openmrs.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.context.Context;
import org.openmrs.cohort.CohortSearchHistory;
import org.openmrs.logic.LogicCriteria;
import org.openmrs.propertyeditor.CohortEditor;
import org.openmrs.propertyeditor.ConceptEditor;
import org.openmrs.propertyeditor.DrugEditor;
import org.openmrs.propertyeditor.EncounterTypeEditor;
import org.openmrs.propertyeditor.FormEditor;
import org.openmrs.propertyeditor.LocationEditor;
import org.openmrs.propertyeditor.PersonAttributeTypeEditor;
import org.openmrs.propertyeditor.ProgramEditor;
import org.openmrs.propertyeditor.ProgramWorkflowStateEditor;
import org.openmrs.report.EvaluationContext;
import org.openmrs.reporting.CohortFilter;
import org.openmrs.reporting.PatientFilter;
import org.openmrs.reporting.PatientSearch;
import org.openmrs.reporting.PatientSearchReportObject;
import org.openmrs.reporting.ReportObjectService;
import org.openmrs.reporting.SearchArgument;
import org.springframework.beans.propertyeditors.CustomDateEditor;

public class ReportingcompatibilityUtil {
	
	private static Log log = LogFactory.getLog(ReportingcompatibilityUtil.class);
	
	public static PatientFilter toPatientFilter(PatientSearch search, CohortSearchHistory history) {
		return toPatientFilter(search, history, null);
	}
	
	/**
	 * Uses reflection to translate a PatientSearch into a PatientFilter
	 */
	@SuppressWarnings("unchecked")
	public static PatientFilter toPatientFilter(PatientSearch search, CohortSearchHistory history,
			EvaluationContext evalContext) {
		if (search.isSavedSearchReference()) {
			PatientSearch ps = ((PatientSearchReportObject) Context.getService(ReportObjectService.class).getReportObject(
				search.getSavedSearchId())).getPatientSearch();
			return toPatientFilter(ps, history, evalContext);
		} else if (search.isSavedFilterReference()) {
			return Context.getService(ReportObjectService.class).getPatientFilterById(search.getSavedFilterId());
		} else if (search.isSavedCohortReference()) {
			Cohort c = Context.getCohortService().getCohort(search.getSavedCohortId());
			// to prevent lazy loading exceptions, cache the member ids here
			if (c != null) {
				c.getMemberIds().size();
			}
			return new CohortFilter(c);
		} else if (search.isComposition()) {
			if (history == null && search.requiresHistory()) {
				throw new IllegalArgumentException("You can't evaluate this search without a history");
			} else {
				return search.cloneCompositionAsFilter(history, evalContext);
			}
		} else {
			Class clz = search.getFilterClass();
			if (clz == null) {
				throw new IllegalArgumentException("search must be saved, composition, or must have a class specified");
			}
			log.debug("About to instantiate " + clz);
			PatientFilter pf = null;
			try {
				pf = (PatientFilter) clz.newInstance();
			}
			catch (Exception ex) {
				log.error("Couldn't instantiate a " + search.getFilterClass(), ex);
				return null;
			}
			Class[] stringSingleton = { String.class };
			if (search.getArguments() != null) {
				for (SearchArgument sa : search.getArguments()) {
					if (log.isDebugEnabled()) {
						log.debug("Looking at (" + sa.getPropertyClass() + ") " + sa.getName() + " -> " + sa.getValue());
					}
					PropertyDescriptor pd = null;
					try {
						pd = new PropertyDescriptor(sa.getName(), clz);
					}
					catch (IntrospectionException ex) {
						log.error("Error while examining property " + sa.getName(), ex);
						continue;
					}
					Class<?> realPropertyType = pd.getPropertyType();
					
					// instantiate the value of the search argument
					String valueAsString = sa.getValue();
					String testForExpression = search.getArgumentValue(sa.getName());
					if (testForExpression != null) {
						log.debug("Setting " + sa.getName() + " to: " + testForExpression);
						if (evalContext != null && EvaluationContext.isExpression(testForExpression)) {
							Object evaluated = evalContext.evaluateExpression(testForExpression);
							if (evaluated != null) {
								if (evaluated instanceof Date) {
									valueAsString = Context.getDateFormat().format((Date) evaluated);
								} else {
									valueAsString = evaluated.toString();
								}
							}
							log.debug("Evaluated " + sa.getName() + " to: " + valueAsString);
						}
					}
					
					Object value = null;
					Class<?> valueClass = sa.getPropertyClass();
					try {
						// If there's a valueOf(String) method, just use that
						// (will cover at least String, Integer, Double,
						// Boolean)
						Method valueOfMethod = null;
						try {
							valueOfMethod = valueClass.getMethod("valueOf", stringSingleton);
						}
						catch (NoSuchMethodException ex) {}
						if (valueOfMethod != null) {
							Object[] holder = { valueAsString };
							value = valueOfMethod.invoke(pf, holder);
						} else if (realPropertyType.isEnum()) {
							// Special-case for enum types
							List<Enum> constants = Arrays.asList((Enum[]) realPropertyType.getEnumConstants());
							for (Enum e : constants) {
								if (e.toString().equals(valueAsString)) {
									value = e;
									break;
								}
							}
						} else if (String.class.equals(valueClass)) {
							value = valueAsString;
						} else if (Location.class.equals(valueClass)) {
							LocationEditor ed = new LocationEditor();
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (Concept.class.equals(valueClass)) {
							ConceptEditor ed = new ConceptEditor();
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (Program.class.equals(valueClass)) {
							ProgramEditor ed = new ProgramEditor();
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (ProgramWorkflowState.class.equals(valueClass)) {
							ProgramWorkflowStateEditor ed = new ProgramWorkflowStateEditor();
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (EncounterType.class.equals(valueClass)) {
							EncounterTypeEditor ed = new EncounterTypeEditor();
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (Form.class.equals(valueClass)) {
							FormEditor ed = new FormEditor();
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (Drug.class.equals(valueClass)) {
							DrugEditor ed = new DrugEditor();
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (PersonAttributeType.class.equals(valueClass)) {
							PersonAttributeTypeEditor ed = new PersonAttributeTypeEditor();
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (Cohort.class.equals(valueClass)) {
							CohortEditor ed = new CohortEditor();
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (Date.class.equals(valueClass)) {
							// TODO: this uses the date format from the current
							// session, which could cause problems if the user
							// changes it after searching.
							DateFormat df = Context.getDateFormat(); // new
							// SimpleDateFormat(OpenmrsConstants.OPENMRS_LOCALE_DATE_PATTERNS().get(Context.getLocale().toString().toLowerCase()),
							// Context.getLocale());
							CustomDateEditor ed = new CustomDateEditor(df, true, 10);
							ed.setAsText(valueAsString);
							value = ed.getValue();
						} else if (LogicCriteria.class.equals(valueClass)) {
							value = Context.getLogicService().parse(valueAsString);
						} else {
							// TODO: Decide whether this is a hack. Currently
							// setting Object arguments with a String
							value = valueAsString;
						}
					}
					catch (Exception ex) {
						log.error("error converting \"" + valueAsString + "\" to " + valueClass, ex);
						continue;
					}
					
					if (value != null) {
						
						if (realPropertyType.isAssignableFrom(valueClass)) {
							log.debug("setting value of " + sa.getName() + " to " + value);
							try {
								pd.getWriteMethod().invoke(pf, value);
							}
							catch (Exception ex) {
								log.error(
									"Error setting value of " + sa.getName() + " to " + sa.getValue() + " -> " + value, ex);
								continue;
							}
						} else if (Collection.class.isAssignableFrom(realPropertyType)) {
							log.debug(sa.getName() + " is a Collection property");
							// if realPropertyType is a collection, add this
							// value to it (possibly after instantiating)
							try {
								Collection collection = (Collection) pd.getReadMethod().invoke(pf, (Object[]) null);
								if (collection == null) {
									// we need to instantiate this collection.
									// I'm going with the following rules, which
									// should be rethought:
									// SortedSet -> TreeSet
									// Set -> HashSet
									// Otherwise -> ArrayList
									if (SortedSet.class.isAssignableFrom(realPropertyType)) {
										collection = new TreeSet();
										log.debug("instantiated a TreeSet");
										pd.getWriteMethod().invoke(pf, collection);
									} else if (Set.class.isAssignableFrom(realPropertyType)) {
										collection = new HashSet();
										log.debug("instantiated a HashSet");
										pd.getWriteMethod().invoke(pf, collection);
									} else {
										collection = new ArrayList();
										log.debug("instantiated an ArrayList");
										pd.getWriteMethod().invoke(pf, collection);
									}
								}
								collection.add(value);
							}
							catch (Exception ex) {
								log.error("Error instantiating collection for property " + sa.getName() + " whose class is "
										+ realPropertyType, ex);
								continue;
							}
						} else {
							log.error(pf.getClass() + " . " + sa.getName() + " should be " + realPropertyType
									+ " but is given as " + valueClass);
						}
					}
				}
			}
			log.debug("Returning " + pf);
			return pf;
		}
	}
}
