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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.cohort.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.cohort.CohortDefinition;
import org.openmrs.reporting.PatientCharacteristicFilter;
import org.openmrs.reporting.PatientSearch;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.openmrs.test.Verifies;

import java.sql.SQLException;


@SkipBaseSetup
public class CohortServiceTest extends BaseModuleContextSensitiveTest {
	
	protected static final String CREATE_PATIENT_XML = "org/openmrs/api/include/PatientServiceTest-createPatient.xml";

	protected static CohortService service = null;
	/**
	 * Run this before each unit test in this class. The "@Before" method in
	 * {@link org.openmrs.test.BaseContextSensitiveTest} is run right before this method.
	 *
     */
	@Before
	public void runBeforeAllTests() throws SQLException {
		initializeInMemoryDatabase();
		authenticate();
		executeDataSet(CREATE_PATIENT_XML);
		service = Context.getService(CohortService.class);
	}
	/**
	 * @see {@link org.openmrs.module.reportingcompatibility.service.CohortService#evaluate(CohortDefinition, org.openmrs.report.EvaluationContext)}
	 */
	@Test
	@Verifies(value = "should return all patients with blank patient search cohort definition provider", method = "evaluate(CohortDefinition,EvaluationContext)")
	public void evaluate_shouldReturnAllPatientsWithBlankPatientSearchCohortDefinitionProvider() throws Exception {
		CohortDefinition def = PatientSearch.createFilterSearch(PatientCharacteristicFilter.class);
		Cohort result = service.evaluate(def, null);
		Assert.assertNotNull("Should not return null", result);
		Assert.assertEquals("Should return one member", 1, result.size());
	}
}
