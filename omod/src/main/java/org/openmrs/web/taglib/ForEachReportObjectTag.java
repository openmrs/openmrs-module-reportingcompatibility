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
package org.openmrs.web.taglib;

import java.util.Iterator;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.openmrs.api.context.Context;
import org.openmrs.module.reportingcompatibility.service.ReportService;
import org.openmrs.reporting.ReportObjectService;

public class ForEachReportObjectTag extends BodyTagSupport {
	
	public static final long serialVersionUID = 1232300L;

	private String name;
	
	private Object select;
	
	private String reportObjectType;
	
	private Iterator<?> records;
	
	public int doStartTag() {
		
		records = null;
		
		if (name.equals("reportSchemaXml")) {
			ReportService svc = (ReportService) Context.getService(ReportService.class);
			records = svc.getReportSchemaXmls().iterator();
		} 
		else if (name.equals("reportObject")) {
			ReportObjectService svc = (ReportObjectService) Context.getService(ReportObjectService.class);
			if (reportObjectType != null) {
				records = svc.getReportObjectsByType(reportObjectType).iterator();
			} 
			else {
				records = svc.getAllReportObjects().iterator();
			}
		}
		
		if (records == null || records.hasNext() == false) {
			records = null;
			return SKIP_BODY;
		} 
		else {
			return EVAL_BODY_BUFFERED;
		}
		
	}
	
	/**
	 * @see javax.servlet.jsp.tagext.BodyTag#doInitBody()
	 */
	public void doInitBody() throws JspException {
		if (records.hasNext()) {
			Object obj = records.next();
			iterate(obj);
		}
	}
	
	/**
	 * @see javax.servlet.jsp.tagext.IterationTag#doAfterBody()
	 */
	public int doAfterBody() throws JspException {
		if (records.hasNext()) {
			Object obj = records.next();
			iterate(obj);
			return EVAL_BODY_BUFFERED;
		} else
			return SKIP_BODY;
	}
	
	private void iterate(Object obj) {
		if (obj != null) {
			pageContext.setAttribute("record", obj);
			pageContext.setAttribute("selected", obj.equals(select) ? "selected" : "");
		} else {
			pageContext.removeAttribute("record");
			pageContext.removeAttribute("selected");
		}
	}
	
	/**
	 * @see javax.servlet.jsp.tagext.Tag#doEndTag()
	 */
	public int doEndTag() throws JspException {
		try {
			if (getBodyContent() != null && records != null)
				getBodyContent().writeOut(getBodyContent().getEnclosingWriter());
		}
		catch (java.io.IOException e) {
			throw new JspTagException("IO Error: " + e.getMessage());
		}
		return EVAL_PAGE;
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param the name
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * @return Returns the select.
	 */
	public Object getSelect() {
		return select;
	}
	
	/**
	 * @param select The select to set.
	 */
	public void setSelect(Object select) {
		this.select = select;
	}
	
	/**
	 * @return the ReportObjectType
	 */
	public String getReportObjectType() {
		return reportObjectType;
	}
	
	/**
	 * @param the ReportObjectType
	 */
	public void setReportObjectType(String reportObjectType) {
		this.reportObjectType = reportObjectType;
	}
}
