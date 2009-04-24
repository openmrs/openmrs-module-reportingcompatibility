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
package org.openmrs.module.reportingcompatibility.extension.html;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openmrs.module.Extension;
import org.openmrs.module.web.extension.AdministrationSectionExt;

/**
 * This class defines the links that will appear on the administration page
 * 
 * This extension is enabled by defining (uncommenting) it in the 
 * /metadata/config.xml file. 
 */
public class AdminList extends AdministrationSectionExt {

	/**
	 * @see org.openmrs.module.web.extension.AdministrationSectionExt#getMediaType()
	 */
	public Extension.MEDIA_TYPE getMediaType() {
		return Extension.MEDIA_TYPE.html;
	}
	
	/**
	 * @see AdministrationSectionExt#getRequiredPrivilege()
	 */
	@Override
	public String getRequiredPrivilege() {
		return "Run Reports,Manage Reports,View Reports,View Data Exports";
	}

	/**
	 * @see org.openmrs.module.web.extension.AdministrationSectionExt#getTitle()
	 */
	public String getTitle() {
		return "reportingcompatibility.Report.header";
	}

	/**
	 * @see org.openmrs.module.web.extension.AdministrationSectionExt#getLinks()
	 */
	public Map<String, String> getLinks() {
		
		Map<String, String> map = new LinkedHashMap<String, String>();
		
		map.put("/admin/reports/runReport.list", "reportingcompatibility.Report.list.title");
		map.put("/admin/reports/reportSchemaXml.list", "reportingcompatibility.Report.manage.title");
		map.put("/admin/reports/reportMacros.form", "reportingcompatibility.Report.macros.title");
		map.put("/admin/reports/dataExport.list", "reportingcompatibility.DataExport.manage");
		map.put("/admin/reports/rowPerObsDataExport.list", "reportingcompatibility.RowPerObsDataExport.manage");
		map.put("/admin/reports/cohorts.list", "Cohort.manage");
		map.put("/admin/reports/patientSearch.list", "reportingcompatibility.PatientSearch.manage");
		map.put("/admin/reports/reportObject.list", "reportingcompatibility.ReportObject.manage");

		return map;
	}
	
}
