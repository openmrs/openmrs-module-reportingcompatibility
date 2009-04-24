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
package org.openmrs.module.reportingcompatibility.db;

import org.openmrs.api.db.DAOException;
import org.openmrs.reporting.AbstractReportObject;
import org.openmrs.reporting.Report;

/**
 * Database methods for the ReportingCompatibilityService
 */
public interface ReportingCompatibilityDAO {
	
	/**
	 * Create a new Report
	 * 
	 * @param r Report to create
	 * @throws DAOException
	 */
	public void createReport(Report r) throws DAOException;
	
	/**
	 * Update Report
	 * 
	 * @param r Report to update
	 * @throws DAOException
	 */
	public void updateReport(Report r) throws DAOException;
	
	/**
	 * Delete Report
	 * 
	 * @param r Report to delete
	 * @throws DAOException
	 */
	public void deleteReport(Report r) throws DAOException;
	
	/**
	 * Create a new Report Object
	 * 
	 * @param ro AbstractReportObject to create
	 * @throws DAOException
	 */
	public void createReportObject(AbstractReportObject ro) throws DAOException;
	
	/**
	 * Update Report Object
	 * 
	 * @param ro AbstractReportObject to update
	 * @throws DAOException
	 */
	public void updateReportObject(AbstractReportObject ro) throws DAOException;
	
	/**
	 * Delete Report Object
	 * 
	 * @param reportObjectId Internal identifier for report object to delete
	 * @throws DAOException
	 */
	public void deleteReportObject(Integer reportObjectId) throws DAOException;
}
