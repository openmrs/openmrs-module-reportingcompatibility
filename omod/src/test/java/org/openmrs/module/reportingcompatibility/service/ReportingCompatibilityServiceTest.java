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
package org.openmrs.module.reportingcompatibility.service;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openmrs.Cohort;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class ReportingCompatibilityServiceTest extends BaseModuleContextSensitiveTest {
	
	private ReportingCompatibilityService rcs = null;
	
	private PatientService ps = null;
	
	@Before
	public void before() throws Exception {
		executeDataSet("org/openmrs/api/include/EncounterServiceTest-initialData.xml");
		rcs = Context.getService(ReportingCompatibilityService.class);
		ps = Context.getPatientService();
	}
	
	/**
	 * @see {@link ReportingCompatibilityService#getEncounters(Cohort)}
	 */
	@Test
	@Verifies(value = "should exclude voided encounters", method = "getEncounters(Cohort)")
	public void getEncounters_shouldExcludeVoidedEncounters() throws Exception {
		List<Patient> patients = ps.getAllPatients(false);
		Cohort cohort = new Cohort();
		for (Patient patient : patients)
			cohort.addMember(patient.getPatientId());
		
		//sanity check that at least there is a patient with a voided encounter
		Patient p = ps.getPatient(3);
		boolean foundVoided = false;
		List<Encounter> encs = Context.getEncounterService().getEncountersByPatient(
		    p.getPatientIdentifier().getIdentifier(), true);
		for (Encounter encounter : encs) {
			if (encounter.isVoided()) {
				foundVoided = true;
				break;
			}
		}
		Assert.assertTrue("At least one voided encounter should be present for one of the patients", foundVoided);
		
		Map<Integer, Encounter> patientEncountersMap = rcs.getEncounters(cohort);
		for (Encounter encounter : patientEncountersMap.values()) {
			Assert.assertFalse(encounter.isVoided());
		}
	}
	
	/**
	 * @see {@link ReportingCompatibilityService#getEncountersByType(Cohort,List<QEncounterType;>)}
	 */
	@Test
	@Verifies(value = "should exclude voided encounters", method = "getEncountersByType(Cohort,List<QEncounterType;>)")
	public void getEncountersByType_shouldExcludeVoidedEncounters() throws Exception {
		List<Patient> patients = ps.getAllPatients(false);
		Cohort cohort = new Cohort();
		for (Patient patient : patients)
			cohort.addMember(patient.getPatientId());
		
		List<EncounterType> encTypes = Context.getEncounterService().getAllEncounterTypes(false);
		
		Map<Integer, Encounter> patientEncountersMap = rcs.getEncountersByType(cohort, encTypes);
		for (Encounter encounter : patientEncountersMap.values()) {
			Assert.assertFalse(encounter.isVoided());
		}
	}
	
	/**
	 * @see {@link ReportingCompatibilityService#getFirstEncountersByType(Cohort,EncounterType)}
	 */
	@Test
	@Verifies(value = "should exclude voided encounters", method = "getFirstEncountersByType(Cohort,EncounterType)")
	public void getFirstEncountersByType_shouldExcludeVoidedEncounters() throws Exception {
		List<Patient> patients = ps.getAllPatients();
		Cohort cohort = new Cohort();
		for (Patient patient : patients)
			cohort.addMember(patient.getPatientId());
		
		List<EncounterType> encTypes = Context.getEncounterService().getAllEncounterTypes(false);
		
		Map<Integer, Encounter> patientEncountersMap = rcs.getFirstEncountersByType(cohort, encTypes);
		for (Encounter encounter : patientEncountersMap.values()) {
			Assert.assertFalse(encounter.isVoided());
		}
	}
}
