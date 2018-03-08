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
package org.openmrs.reporting;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.module.reportingcompatibility.service.ReportService;
import org.openmrs.report.EvaluationContext;

public abstract class CachingPatientFilter extends AbstractPatientFilter implements PatientFilter {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Subclasses should implement PatientFilter.filter("all patients", evalContext) in this method
	 *
	 * @param context
	 * @return TODO
	 */
	public abstract Cohort filterImpl(EvaluationContext context);
	
	/**
	 * @return The key under which this object, with its current parameter values, will store
	 *         results in a cache. Changing properties of this object will typically change the
	 *         cache key returned.
	 */
	public abstract String getCacheKey();
	
	/**
	 * TODO Auto generated method comment
	 *
	 * @param context
	 * @return
	 */
	private Cohort getAndMaybeCache(EvaluationContext context) {
		if (context == null) {
			return filterImpl(null);
		} else {
			String key = getCacheKey();
			Cohort cached = (Cohort) context.getFromCache(key);
			if (cached == null) {
				cached = filterImpl(context);
				context.addToCache(key, cached);
			}
			return cached;
		}
	}
	
	/**
	 * @see org.openmrs.reporting.PatientFilter#filter(org.openmrs.Cohort,
	 *      org.openmrs.report.EvaluationContext)
	 */
	public Cohort filter(Cohort input, EvaluationContext context) {
		Cohort cached = getAndMaybeCache(context);
		if (input == null) {
			if (context != null) {
				input = context.getBaseCohort();
			} else {
				input = Context.getService(ReportService.class).getAllPatients();
			}
		}
		
		if (isTwoPointOneOrAbove()) {
			Date date = new Date();
			makeStartDateAndUuidTheSame(input, date);
			makeStartDateAndUuidTheSame(cached, date);
		}
		
		return Cohort.intersect(input, cached);
	}
	
	private void makeStartDateAndUuidTheSame(Cohort cohort, Date date) {
		if (cohort == null) {
			return;
		}
		
		try {
			Method method = cohort.getClass().getMethod("getMemberships", null);
			Collection memberships = (Collection)method.invoke(cohort, null);
			Iterator iterator = memberships.iterator();
			while (iterator.hasNext()) {
				Object membership = iterator.next();
				
				method = membership.getClass().getMethod("setStartDate", new Class[] { Date.class });
				method.invoke(membership, new Object[] { date });
				
				method = membership.getClass().getMethod("setUuid", new Class[] { String.class });
				method.invoke(membership, new Object[] { "6f0c9a92-6f24-11e3-af88-005056821db0" });
			}
		}
		catch (Exception e) {
			log.error(e);
		}
	}
	
	private static boolean isTwoPointOneOrAbove() {
		try {
			Context.loadClass("org.openmrs.CohortMembership");
			return true;
		}
		catch (ClassNotFoundException e) {}
		return false;
	}
	
	/**
	 * @see org.openmrs.reporting.PatientFilter#filterInverse(org.openmrs.Cohort,
	 *      org.openmrs.report.EvaluationContext)
	 */
	public Cohort filterInverse(Cohort input, EvaluationContext context) {
		Cohort cached = getAndMaybeCache(context);
		if (input == null) {
			if (context != null) {
				input = context.getBaseCohort();
			} else {
				input = Context.getService(ReportService.class).getAllPatients();
			}
		}
		return Cohort.subtract(input, cached);
	}
	
	/**
	 * @see org.openmrs.reporting.PatientFilter#isReadyToRun()
	 */
	public abstract boolean isReadyToRun();
	
}
