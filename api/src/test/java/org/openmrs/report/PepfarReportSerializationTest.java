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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.cohort.Cohort;
import org.openmrs.ConceptClass;
import org.openmrs.Location;
import org.openmrs.User;
import org.openmrs.api.context.Context;
import org.openmrs.cohort.StaticCohortDefinition;
import org.openmrs.reporting.PatientSearch;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.TestUtil;
import org.xmlunit.assertj.XmlAssert;

/**
 * Test class that tests the serialization and deserialization of the a very simple pepfar report
 */
@Ignore("We need to get some time to make this test work on platform 2.0")
public class PepfarReportSerializationTest extends BaseModuleContextSensitiveTest {
	
	/**
	 * Set up the database with the initial dataset before every test method in this class.
	 */
	@Before
	public void runBeforeEachTest() throws Exception {
		executeDataSet("org/openmrs/report/include/ReportSchemaXmlTest-initialData.xml");
	}
	
	/**
	 * Creates a basic pepfar report schema and makes sure it can be serialized correctly
	 * 
	 * @throws Exception
	 */
	@Test
	public void shouldPepfarStaticCohortSerialization() throws Exception {
		
		// the report schema object to serialize
		ReportSchema pepfarReportSchema = new ReportSchema();
		
		// create the columns of the report
		List<DataSetDefinition> dataSets = new ArrayList<DataSetDefinition>();
		CohortDataSetDefinition cohortDataSetDef = new CohortDataSetDefinition();
		cohortDataSetDef.setName("Sheet1");
		
		cohortDataSetDef.addStrategy("1.a", new StaticCohortDefinition(new Cohort()));
		pepfarReportSchema.setDataSetDefinitions(dataSets);
		
		// add the parameters of the report
		Parameter startDateParam = new Parameter("report.startDate", "When does the report period start?", Date.class, null);
		Parameter endDateParam = new Parameter("report.endDate", "When does the report period end?", Date.class, null);
		Parameter locationParam = new Parameter("report.location", "For which clinic is this report?", Location.class, null);
		List<Parameter> parameters = new ArrayList<Parameter>();
		parameters.add(startDateParam);
		parameters.add(endDateParam);
		parameters.add(locationParam);
		pepfarReportSchema.setReportParameters(parameters);
		
		// add the rows (cohort) to the report
		Cohort inputStaticCohort = new Cohort(1);
		inputStaticCohort.addMember(1001);
		inputStaticCohort.addMember(1002);
		inputStaticCohort.addMember(1003);
		pepfarReportSchema.setFilter(new StaticCohortDefinition(inputStaticCohort));
		
		// finally, fill in the details of the report
		pepfarReportSchema.setReportSchemaId(123);
		pepfarReportSchema.setName("PEPFAR report");
		pepfarReportSchema.setDescription("The PEPFAR description is(n't) here.");
		
		// do the serialization
		String xmlOutput = Context.getSerializationService().getDefaultSerializer().serialize(pepfarReportSchema);
		
		String correctOutput = "<reportSchema id=\"1\" reportSchemaId=\"123\">\n"
		        + "   <filter class=\"org.openmrs.cohort.StaticCohortDefinition\" id=\"2\">\n"
		        + "      <cohort id=\"3\" voided=\"false\" cohortId=\"1\">\n"
		        + "         <memberIds class=\"java.util.HashSet\" id=\"4\">\n"
		        + "            <integer id=\"5\">1002</integer>\n" + "            <integer id=\"6\">1003</integer>\n"
		        + "            <integer id=\"7\">1001</integer>\n" + "         </memberIds>\n" + "      </cohort>\n"
		        + "   </filter>\n" + "   <parameters class=\"java.util.ArrayList\" id=\"8\">\n"
		        + "      <parameter id=\"9\" clazz=\"java.util.Date\">\n"
		        + "         <label id=\"10\"><![CDATA[When does the report period start?]]></label>\n"
		        + "         <name id=\"11\"><![CDATA[report.startDate]]></name>\n" + "      </parameter>\n"
		        + "      <parameter id=\"12\" clazz=\"java.util.Date\">\n"
		        + "         <label id=\"13\"><![CDATA[When does the report period end?]]></label>\n"
		        + "         <name id=\"14\"><![CDATA[report.endDate]]></name>\n" + "      </parameter>\n"
		        + "      <parameter id=\"15\" clazz=\"org.openmrs.Location\">\n"
		        + "         <label id=\"16\"><![CDATA[For which clinic is this report?]]></label>\n"
		        + "         <name id=\"17\"><![CDATA[report.location]]></name>\n" + "      </parameter>\n"
		        + "   </parameters>\n"
		        + "   <description id=\"18\"><![CDATA[The PEPFAR description is(n't) here.]]></description>\n"
		        + "   <dataSets class=\"java.util.ArrayList\" id=\"19\"/>\n"
		        + "   <name id=\"20\"><![CDATA[PEPFAR report]]></name>\n" + "</reportSchema>";

		XmlAssert.assertThat(xmlOutput).valueByXPath("//reportSchema/filter/@class").isEqualTo("org.openmrs.cohort.StaticCohortDefinition");

		// TODO how to just test to see if 1001, 1002, and 1003 all exist as values in memberIds/integer?
		XmlAssert.assertThat(xmlOutput).valueByXPath("//reportSchema/filter/cohort/memberIds/integer").isEqualTo("1001");

		// check some simple deserialized value
		ReportSchema deserializedSchema = Context.getSerializationService().getDefaultSerializer().deserialize(correctOutput, ReportSchema.class);
		assertTrue("The # of params shouldn't be: " + deserializedSchema.getReportParameters().size(), deserializedSchema
		        .getReportParameters().size() == 3);
		assertTrue("The name shouldn't be: " + deserializedSchema.getName(), deserializedSchema.getName().equals(
		    "PEPFAR report"));
		Cohort idsFromStaticCohort = ((StaticCohortDefinition) deserializedSchema.getFilter()).getCohort();
		int size = idsFromStaticCohort.getMemberIds().size();
		assertTrue("There should be 3 patients in the static cohort, not: " + size, size == 3);
	}
	
	/**
	 * TODO: move this out of the pepfarReportSerializationTest class
	 */
	@Test
	public void shouldCohortSerialization() throws Exception {
		
		PatientSearch cohortByPatientSearch = PatientSearch.createCompositionSearch("[1] AND [2]");
		
		
		//serializer.write(cohortByPatientSearch, writer);
		
		//System.out.println(writer.toString());
		
	}
	
	/**
	 * TODO: move this out of the pepfarReportSerializationTest class
	 */
	@Test
	public void shouldConceptClassShortSerialization() throws Exception {
		User user = new User(1);
		user.setSystemId("systemId1");
		user.setCreator(user);
		user.setDateCreated(new Date());
		
		ConceptClass conceptClass = new ConceptClass(123);
		conceptClass.setCreator(user);
		conceptClass.setDateCreated(new Date());
		conceptClass.setDescription("This is the description");
		conceptClass.setName("This is the name");
		
		String xml = Context.getSerializationService().getDefaultSerializer().serialize(conceptClass);
		
		TestUtil.printAssignableToSingleString(xml);
		
		//System.out.println("FULL:" + writer.toString());
		
	}
	
	@Test
	public void shouldConceptClassDeserialization() throws Exception {
		String serializedClass = "<conceptClass id=\"0\" conceptClassId=\"123\">\n   <description id=\"1\"><![CDATA[This is the description]]></description>\n   <name id=\"2\"><![CDATA[This is the name]]></name>\n   <dateCreated id=\"3\">2007-12-17 14:00:01.515 EST</dateCreated>\n   <creator id=\"4\" birthdateEstimated=\"false\" voided=\"false\" userId=\"1\" personId=\"1\" dead=\"false\">\n      <names class=\"java.util.TreeSet\" id=\"5\"/>\n      <attributes class=\"java.util.TreeSet\" id=\"6\"/>\n      <addresses class=\"java.util.TreeSet\" id=\"7\"/>\n   </creator>\n</conceptClass>";
		ConceptClass read = Context.getSerializationService().getDefaultSerializer().deserialize(serializedClass, ConceptClass.class);
	}
	
}
