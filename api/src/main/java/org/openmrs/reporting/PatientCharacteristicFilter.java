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

import java.text.DateFormat;
import java.util.Date;

import org.openmrs.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.reportingcompatibility.service.ReportService;
import org.openmrs.report.EvaluationContext;

public class PatientCharacteristicFilter extends CachingPatientFilter implements Comparable<PatientCharacteristicFilter> {
	
	private String gender;
	
	private Date minBirthdate;
	
	private Date maxBirthdate;
	
	private Date minDeathdate;
	
	private Date maxDeathdate;
	
	private Integer minAge;
	
	private Integer maxAge;
	
	private Boolean aliveOnly;
	
	private Boolean deadOnly;
	
	private Date effectiveDate;
	
	public PatientCharacteristicFilter() {
		super.setType("Patient Filter");
		super.setSubType("Patient Characteristic Filter");
	}
	
	public PatientCharacteristicFilter(String gender, Date minBirthdate, Date maxBirthdate) {
		super.setType("Patient Filter");
		super.setSubType("Patient Characteristic Filter");
		this.gender = gender == null ? null : gender.toUpperCase();
		this.minBirthdate = minBirthdate;
		this.maxBirthdate = maxBirthdate;
	}
	
	public PatientCharacteristicFilter(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate) {
		super.setType("Patient Filter");
		super.setSubType("Patient Characteristic Filter");
		this.gender = gender == null ? null : gender.toUpperCase();
		this.minBirthdate = minBirthdate;
		this.maxBirthdate = maxBirthdate;
		this.minDeathdate = minDeathdate;
		this.maxDeathdate = maxDeathdate;
	
	}
	
	@Override
	public String getCacheKey() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName()).append(".");
		sb.append(getGender()).append(".");
		sb.append(getMinBirthdate()).append(".");
		sb.append(getMaxBirthdate()).append(".");
		sb.append(getMinDeathdate()).append(".");
		sb.append(getMaxDeathdate()).append(".");
		sb.append(getMinAge()).append(".");
		sb.append(getMaxAge()).append(".");
		sb.append(getAliveOnly()).append(".");
		sb.append(getDeadOnly()).append(".");
		sb.append(getEffectiveDate());
		return sb.toString();
	}
	
	public boolean isReadyToRun() {
		return true;
	}
	
	public int compareTo(PatientCharacteristicFilter o) {
		return -compareHelper().compareTo(o.compareHelper());
	}
	
	private Integer compareHelper() {
		int ret = 0;
		if (deadOnly != null) {
			ret += deadOnly ? 2 : 1;
		}
		if (aliveOnly != null) {
			ret += aliveOnly ? 20 : 10;
		}
		if (minAge != null) {
			ret += minAge * 100;
		}
		if (maxAge != null) {
			ret += maxAge * 1000;
		}
		if (gender != null) {
			ret += gender.equals("M") ? 1000000 : 2000000;
		}
		return ret;
	}
	
	public String getDescription() {
		MessageSourceService msa = Context.getMessageSourceService();
		if (gender == null && minBirthdate == null && maxBirthdate  == null && minDeathdate == null && maxDeathdate == null && minAge == null && maxAge == null
		        && aliveOnly == null && deadOnly == null) {
			return msa.getMessage("reporting.allPatients");
		}
		
		StringBuffer ret = new StringBuffer();
		if (gender != null) {
			if ("M".equals(gender)) {
				ret.append(msa.getMessage("reporting.male"));
			} else {
				ret.append(msa.getMessage("reporting.female"));
			}
		}
		ret.append(gender == null ? msa.getMessage("reporting.patients") + " " : " "
		        + msa.getMessage("reporting.patients").toLowerCase() + " ");
		
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Context.getLocale());
		
		if (minBirthdate != null) {
			if (maxBirthdate != null) {
				ret.append(" ").append(
				    msa.getMessage("reporting.bornBetween", new Object[] { (Object) df.format(minBirthdate),
				            (Object) df.format(maxBirthdate) }, Context.getLocale()));
			} else {
				ret.append(" ").append(msa.getMessage("reporting.bornAfter")).append(" ").append(df.format(minBirthdate));
			}
		} else {
			if (maxBirthdate != null) {
				ret.append(" ").append(msa.getMessage("reporting.bornBefore")).append(" ").append(df.format(maxBirthdate));
			}
		}
		
		if (minDeathdate != null) {
			if (maxDeathdate != null) {
				ret.append(" ").append(
						msa.getMessage("reporting.diedBetween", new Object[] { (Object) df.format(minDeathdate),
								(Object) df.format(maxDeathdate) }, Context.getLocale()));
			} else {
				ret.append(" ").append(msa.getMessage("reporting.diedAfter")).append(" ").append(df.format(minDeathdate));
			}
		} else {
			if (maxDeathdate != null) {
				ret.append(" ").append(msa.getMessage("reporting.diedBefore")).append(" ").append(df.format(maxDeathdate));
			}
		}
		
		if (minAge != null) {
			if (maxAge != null) {
				ret.append(" ").append(
				    msa.getMessage("reporting.betweenTheAgesOf", new Object[] { (Object) minAge, (Object) maxAge }, Context
				            .getLocale()));
			} else {
				ret.append(" ").append(
				    msa.getMessage("reporting.atLeastYearsOld", new Object[] { minAge }, Context.getLocale()));
			}
		} else {
			if (maxAge != null) {
				ret.append(" ").append(
				    msa.getMessage("reporting.atMostYearsOld", new Object[] { maxAge }, Context.getLocale()));
			}
		}
		if (aliveOnly != null && aliveOnly) {
			ret.append(" ").append(msa.getMessage("reporting.whoAreAlive"));
		}
		if (deadOnly != null && deadOnly) {
			ret.append(" ").append(msa.getMessage("reporting.whoAreDead"));
		}
		return ret.toString();
	}
	
	/**
	 * @return Returns the gender.
	 */
	public String getGender() {
		return gender;
	}
	
	/**
	 * @param gender The gender to set.
	 */
	public void setGender(String gender) {
		this.gender = null;
		if (gender != null) {
			gender = gender.toUpperCase();
			if ("M".equals(gender) || "F".equals(gender)) {
				this.gender = gender;
			}
		}
	}
	
	/**
	 * @return Returns the maxBirthdate.
	 */
	public Date getMaxBirthdate() {
		return maxBirthdate;
	}
	
	/**
	 * @param maxBirthdate The maxBirthdate to set.
	 */
	public void setMaxBirthdate(Date maxBirthdate) {
		this.maxBirthdate = maxBirthdate;
	}
	
	/**
	 * @return Returns the minBirthdate.
	 */
	public Date getMinBirthdate() {
		return minBirthdate;
	}
	
	/**
	 * @param minBirthdate The minBirthdate to set.
	 */
	public void setMinBirthdate(Date minBirthdate) {
		this.minBirthdate = minBirthdate;
	}
	
	public Boolean getAliveOnly() {
		return aliveOnly;
	}
	
	public void setAliveOnly(Boolean aliveOnly) {
		this.aliveOnly = aliveOnly;
	}
	
	public Boolean getDeadOnly() {
		return deadOnly;
	}
	
	public void setDeadOnly(Boolean deadOnly) {
		this.deadOnly = deadOnly;
	}
	
	public Integer getMaxAge() {
		return maxAge;
	}
	
	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}
	
	public Integer getMinAge() {
		return minAge;
	}
	
	public void setMinAge(Integer minAge) {
		this.minAge = minAge;
	}
	
	public Date getEffectiveDate() {
		return effectiveDate;
	}
	
	public void setEffectiveDate(Date effectiveDate) {
		this.effectiveDate = effectiveDate;
	}
	
	public Date getMinDeathdate() {
		return minDeathdate;
	}
	
	public void setMinDeathdate(Date minDeathdate) {
		this.minDeathdate = minDeathdate;
	}
	
	public Date getMaxDeathdate() {
		return maxDeathdate;
	}
	
	public void setMaxDeathdate(Date maxDeathdate) {
		this.maxDeathdate = maxDeathdate;
	}
	
	@Override
	public Cohort filterImpl(EvaluationContext context) {
		return Context.getService(ReportService.class).getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minDeathdate, maxDeathdate, minAge, maxAge, aliveOnly, deadOnly, effectiveDate);
	}
}