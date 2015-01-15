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
package org.openmrs.module.reportingcompatibility.web.controller.analysis;

import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.openmrs.web.controller.analysis.CohortBuilderController;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

public class CohortBuilderControllerTest extends BaseModuleWebContextSensitiveTest {

	@Test
	public void shouldSortDrugs() throws Exception {
		executeDataSet("org/openmrs/include/drugs.xml");
		CohortBuilderController controller = (CohortBuilderController) applicationContext.getBean("cohortBuilderController");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/openmrs/cohortBuilder.list");
		request.setSession(new MockHttpSession(null));
		HttpServletResponse response = new MockHttpServletResponse();
		controller.handleRequest(request, response);
	}
}
