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
package org.openmrs.report;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.cohort.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.module.reportingcompatibility.service.CohortService;
import org.openmrs.module.reportingcompatibility.service.ReportService;
import org.openmrs.report.impl.TsvReportRenderer;
import org.openmrs.reporting.PatientCharacteristicFilter;
import org.openmrs.reporting.PatientSearch;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.xmlunit.assertj.XmlAssert;

/**
 *
 */
@Ignore("We need to get some time to make this test work on platform 2.0")
public class RowPerObsDatasetTest extends BaseModuleContextSensitiveTest {
	
	private Log log = LogFactory.getLog(getClass());
	
	/**
	 * TODO: fix this so it uses asserts instead of printing to the screen
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldSerialize() throws Exception {
		executeDataSet("org/openmrs/report/include/RowPerObsDatasetTest.xml");
		
		EvaluationContext evalContext = new EvaluationContext();
		PatientSearch kids = PatientSearch.createFilterSearch(PatientCharacteristicFilter.class);
		
		Calendar today = new GregorianCalendar();
		
		// the first patient in the dataset has a birthday in 2007, so subtract that from
		// the current year for that patients age, then add one to make sure that patient
		// is in the cohort (the second patient was born in 2000, so they shouldn't
		// get counted)
		Integer maxAge = today.get(Calendar.YEAR) - 2007 + 1;
		kids.addArgument("maxAge", maxAge.toString(), Integer.class);
		Cohort kidsCohort = Context.getService(CohortService.class).evaluate(kids, evalContext);
		
		RowPerObsDataSetDefinition definition = new RowPerObsDataSetDefinition();
		definition.setName("Row per Obs");
		//commenting this out because serializing PatientSearches is not yet implemented
		//definition.setFilter(kids);
		definition.getQuestions().add(Context.getConceptService().getConcept(5089));
		
		ReportSchema rs = new ReportSchema();
		rs.setName("Testing row-per-obs");
		rs.setDescription("Tesing RowPerObsDataSet*");
		rs.addDataSetDefinition(definition);
		
		String xmlOutput = Context.getSerializationService().getDefaultSerializer().serialize(rs);
		
		String expectedOutput = "<reportSchema id=\"1\">\n   <description id=\"2\"><![CDATA[Tesing RowPerObsDataSet*]]></description>\n   <dataSets class=\"java.util.Vector\" id=\"3\">\n      <dataSetDefinition class=\"org.openmrs.report.RowPerObsDataSetDefinition\" id=\"4\" name=\"Row per Obs\">\n         <questions class=\"java.util.HashSet\" id=\"5\">\n            <concept id=\"6\" conceptId=\"5089\"/>\n         </questions>\n      </dataSetDefinition>\n   </dataSets>\n   <name id=\"7\"><![CDATA[Testing row-per-obs]]></name>\n</reportSchema>";

		XmlAssert.assertThat(xmlOutput).valueByXPath("//reportSchema/dataSets/dataSetDefinition/questions/concept/@conceptId").isEqualTo("5089");

		//log.error("xmlOutput: " + xmlOutput);
		
		rs = Context.getSerializationService().getDefaultSerializer().deserialize(xmlOutput, ReportSchema.class);
		assertEquals("Testing row-per-obs", rs.getName());
		assertEquals(1, rs.getDataSetDefinitions().size());
		
		ReportData data = Context.getService(ReportService.class).evaluate(rs, kidsCohort, evalContext);
		//System.out.println("Result=");
		
		StringWriter w = new StringWriter();
		new TsvReportRenderer().render(data, null, w);
		
		expectedOutput = "\"patientId\"	\"question\"	\"questionConceptId\"	\"answer\"	\"answerConceptId\"	\"obsDatetime\"	\"encounterId\"	\"obsGroupId\"	\n\"2\"	\"WEIGHT (KG)\"	\"5089\"	\"100.0\"	\"\"	\"2005-01-01 00:00:00.0\"	\"1\"	\"\"	\n";
		// (This line was used to generate the above line of code)
		// TestUtil.printAssignableToSingleString(w.toString());
		
		assertEquals(expectedOutput, w.toString());
		
	}
	
}
