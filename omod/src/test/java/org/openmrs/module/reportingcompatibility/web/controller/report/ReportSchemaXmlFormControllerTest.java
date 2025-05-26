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
package org.openmrs.module.reportingcompatibility.web.controller.report;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.rules.ExpectedException;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.reportingcompatibility.service.ReportService;
import org.openmrs.report.ReportConstants;
import org.openmrs.report.ReportSchemaXml;
import org.openmrs.test.TestUtil;
import org.openmrs.web.controller.report.ReportSchemaXmlFormController;
import org.openmrs.web.test.jupiter.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

public class ReportSchemaXmlFormControllerTest extends BaseModuleWebContextSensitiveTest {

    private static final String USERS_DATASET = "org/openmrs/api/include/UserServiceTest.xml";
    private static final String OTHER_TEST_DATA = "org/openmrs/include/ReportSchemaXml-otherTestData.xml";

    private ReportService reportService;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("messageSourceService")
    private MessageSourceService mss;

    @Autowired
    private ReportSchemaXmlFormController controller;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        reportService = Context.getService(ReportService.class);
    }

    @Test
    public void shouldFailForAUserWithoutTheAddReportsPrivilege() throws Exception {
        executeDataSet(USERS_DATASET);
        Context.logout();
        Context.authenticate("correctlyhashedSha1", "test");
        Assert.assertFalse(Context.getAuthenticatedUser().hasPrivilege(ReportConstants.PRIV_ADD_REPORTS));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
        request.setParameter("xml", "<org.openmrs.report.ReportSchema>" +
                "</org.openmrs.report.ReportSchema>");
        expectedException.expect(ContextAuthenticationException.class);
        String expectedMessage = mss.getMessage("error.privilegesRequired",
                new Object[]{ReportConstants.PRIV_ADD_REPORTS}, null);
        expectedException.expectMessage(Matchers.equalTo(expectedMessage));
        controller.handleRequest(request, new MockHttpServletResponse());
    }

    @Test
    public void shouldFailForAUserWithoutTheEditReportsPrivilege() throws Exception {
        executeDataSet(USERS_DATASET);
        ReportSchemaXml schemaXml = new ReportSchemaXml();
        schemaXml.setName("Some name");
        schemaXml.setXml("<org.openmrs.report.ReportSchema>" +
                "</org.openmrs.report.ReportSchema>");
        reportService.saveReportSchemaXml(schemaXml);
        Assert.assertNotNull(schemaXml.getId());

        Context.logout();
        Context.authenticate("correctlyhashedSha1", "test");
        Assert.assertFalse(Context.getAuthenticatedUser().hasPrivilege(ReportConstants.PRIV_EDIT_REPORTS));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
        request.addParameter("reportSchemaId", schemaXml.getId().toString());
        expectedException.expect(ContextAuthenticationException.class);
        String expectedMessage = mss.getMessage("error.privilegesRequired",
                new Object[]{ReportConstants.PRIV_EDIT_REPORTS}, null);
        expectedException.expectMessage(Matchers.equalTo(expectedMessage));
        controller.handleRequest(request, new MockHttpServletResponse());
    }

    @Test
    public void shouldPassForAUserThatHasTheAddReportsPrivilege() throws Exception {
        executeDataSet(USERS_DATASET);
        executeDataSet(OTHER_TEST_DATA);
        Context.logout();
        Context.authenticate("correctlyhashedSha1", "test");
        Assert.assertTrue(Context.getAuthenticatedUser().hasPrivilege(ReportConstants.PRIV_ADD_REPORTS));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
        request.setParameter("xml", "<org.openmrs.report.ReportSchema>" +
                "</org.openmrs.report.ReportSchema>");
        int originalCount = reportService.getReportSchemas().size();
        ModelAndView mav = controller.handleRequest(request, new MockHttpServletResponse());
        Assert.assertNotNull(mav);
        Assert.assertTrue(mav.getModel().isEmpty());
        Assert.assertEquals(++originalCount, reportService.getReportSchemas().size());
        TestUtil.printOutTableContents(getConnection(), "report_schema_xml");
    }

    @Test
    public void shouldPassForAUserThatHasTheEditReportsPrivilege() throws Exception {
        executeDataSet(USERS_DATASET);
        executeDataSet(OTHER_TEST_DATA);
        ReportSchemaXml schemaXml = new ReportSchemaXml();
        schemaXml.setName("Some name");
        schemaXml.setXml("<org.openmrs.report.ReportSchema><reportSchemaId><reportSchemaId>" +
                "</org.openmrs.report.ReportSchema>");
        reportService.saveReportSchemaXml(schemaXml);
        Assert.assertNotNull(schemaXml.getId());

        Context.logout();
        Context.authenticate("correctlyhashedSha1", "test");
        Assert.assertTrue(Context.getAuthenticatedUser().hasPrivilege(ReportConstants.PRIV_EDIT_REPORTS));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "");
        request.addParameter("reportSchemaId", schemaXml.getId().toString());
        ModelAndView mav = controller.handleRequest(request, new MockHttpServletResponse());
        Assert.assertNotNull(mav);
    }
}
