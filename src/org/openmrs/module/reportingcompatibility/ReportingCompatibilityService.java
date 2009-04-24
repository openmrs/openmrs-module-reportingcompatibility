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
package org.openmrs.module.reportingcompatibility;

import java.util.List;
import java.util.Map;

import org.openmrs.Cohort;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.cohort.CohortDefinition;
import org.openmrs.cohort.CohortDefinitionItemHolder;
import org.openmrs.cohort.CohortDefinitionProvider;
import org.openmrs.module.reportingcompatibility.db.ReportingCompatibilityDAO;
import org.openmrs.report.EvaluationContext;
import org.openmrs.reporting.AbstractReportObject;
import org.openmrs.reporting.Report;
import org.springframework.transaction.annotation.Transactional;

/**
 * Contains all service methods required to support compatibility of reporting in 
 * versions 1.5 and earlier.  Core services will delegate to these methods as appropriate.
 */
@Transactional
public interface ReportingCompatibilityService extends OpenmrsService {
	
	/**
	 * Create a new Report
	 * 
	 * @param report Report to create
	 * @throws APIException
	 */
	public void createReport(Report report) throws APIException;
	
	/**
	 * Update Report
	 * 
	 * @param report Report to update
	 * @throws APIException
	 */
	public void updateReport(Report report) throws APIException;
	
	/**
	 * Delete Report
	 * 
	 * @param report Report to delete
	 * @throws APIException
	 */
	public void deleteReport(Report report) throws APIException;
	
	/**
	 * Create a new Report Object
	 * 
	 * @param reportObject Report Object to create
	 * @throws APIException
	 */
	public void createReportObject(AbstractReportObject reportObject) throws APIException;
	
	/**
	 * Update Report Object
	 * 
	 * @param reportObject the Report Object to update
	 * @throws APIException
	 */
	public void updateReportObject(AbstractReportObject reportObject) throws APIException;
	
	/**
	 * Delete Report Object
	 * 
	 * @param reportObjectId Internal identifier for the Report Object to delete
	 * @throws APIException
	 */
	public void deleteReportObject(Integer reportObjectId) throws APIException;
	
	/**
	 * Set the given CohortDefinitionProviders as the providers for this service. These are set via
	 * spring injection in /metadata/spring/applicationContext-service.xml . This method acts more
	 * like an "add" than a "set". All entries in the <code>providerClassMap</code> are added to the
	 * already set list of providers. This allows multiple Spring application context files to call
	 * this method with their own providers
	 * 
	 * @param providerClassMap mapping from CohortDefinition to its provider
	 * @should not overwrite previously set providers if called twice
	 */
	@Transactional(readOnly = true)
	public void setCohortDefinitionProviders(
	                                         Map<Class<? extends CohortDefinition>, CohortDefinitionProvider> providerClassMap)
	                                                                                                                           throws APIException;
	
	/**
	 * Adds the given cohort definition provider to this service's list of providers
	 * 
	 * @param cohortDefClass the type of cohort definition that this provider works on
	 * @param cohortDef the provider
	 * @throws APIException
	 * @should overwrite provider if duplicate CcohortDefinition class
	 */
	@Transactional(readOnly = true)
	public void registerCohortDefinitionProvider(Class<? extends CohortDefinition> cohortDefClass,
	                                             CohortDefinitionProvider cohortDef) throws APIException;
	
	/**
	 * Gets all the providers registered to this service. Will return an empty list instead of null.
	 * 
	 * @return this service's providers
	 * @throws APIException
	 * @see #setCohortDefinitionProviders(Map)
	 * @should not return null if not providers have been set
	 */
	@Transactional(readOnly = true)
	public Map<Class<? extends CohortDefinition>, CohortDefinitionProvider> getCohortDefinitionProviders()
	                                                                                                      throws APIException;
	
	/**
	 * Removing any mapping from CohortDefinition to provider in this server where the given
	 * <code>providerClass</class> is the
	 * CohortDefinitionProvider
	 * 
	 * @param providerClass the provider to remove
	 * @throws APIException
	 * @should not fail if providerClass not set
	 */
	@Transactional(readOnly = true)
	public void removeCohortDefinitionProvider(Class<? extends CohortDefinitionProvider> providerClass) throws APIException;
	
	/**
	 * Get every cohort definition that every registered provider knows about. This is typically
	 * used for selection by an end user, and so a list of Holders are returned instead of a list of
	 * definitions so that the system knows which provider the definition came from
	 * 
	 * @return a list of CohortDefinitionItemHolder defined
	 * @throws APIException
	 */
	@Transactional(readOnly = true)
	public List<CohortDefinitionItemHolder> getAllCohortDefinitions() throws APIException;
	
	@Transactional(readOnly = true)
	public List<CohortDefinitionItemHolder> getCohortDefinitions(Class<? extends CohortDefinitionProvider> providerClass)
	                                                                                                                     throws APIException;
	
	@Transactional(readOnly = true)
	public CohortDefinition getCohortDefinition(Class<CohortDefinition> clazz, Integer id) throws APIException;
	
	@Transactional(readOnly = true)
	public CohortDefinition getCohortDefinition(String cohortKey) throws APIException;
	
	@Transactional(readOnly = true)
	public CohortDefinition saveCohortDefinition(CohortDefinition definition) throws APIException;
	
	@Transactional
	public void purgeCohortDefinition(CohortDefinition definition) throws APIException;
	
	/**
	 * TODO Auto generated method comment
	 * 
	 * @param definition
	 * @param evalContext
	 * @return Cohort determined by the given CohortDefinition and EvaluationContext
	 * @throws APIException
	 * @should return all patients with blank patient search cohort definition provider
	 */
	@Transactional(readOnly = true)
	public Cohort evaluate(CohortDefinition definition, EvaluationContext evalContext) throws APIException;
	
	@Transactional(readOnly = true)
	public CohortDefinition getAllPatientsCohortDefinition() throws APIException;

	/**
	 * Used by Spring to set the specific/chosen database access implementation
	 * @param dao The dao implementation to use
	 */
	public void setReportingCompatibilityDAO(ReportingCompatibilityDAO dao);
}
