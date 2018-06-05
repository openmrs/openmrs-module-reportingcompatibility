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

import org.openmrs.cohort.Cohort;
import org.openmrs.api.APIException;
import org.openmrs.api.OpenmrsService;
import org.openmrs.cohort.CohortDefinition;
import org.openmrs.cohort.CohortDefinitionItemHolder;
import org.openmrs.cohort.CohortDefinitionProvider;
import org.openmrs.report.EvaluationContext;

/**
 * @see org.openmrs.cohort.CohortDefinition
 * @see org.openmrs.cohort.CohortDefinitionProvider
 */
public interface CohortService extends OpenmrsService {
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
	public void setCohortDefinitionProviders(
			Map<Class<? extends CohortDefinition>, CohortDefinitionProvider> providerClassMap) throws APIException;

	/**
	 * Adds the given cohort definition provider to this service's list of providers
	 *
	 * @param cohortDefClass the type of cohort definition that this provider works on
	 * @param cohortDef the provider
	 * @throws APIException
	 * @should overwrite provider if duplicate CcohortDefinition class
	 */
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
	public void removeCohortDefinitionProvider(Class<? extends CohortDefinitionProvider> providerClass) throws APIException;

	/**
	 * Get every cohort definition that every registered provider knows about. This is typically
	 * used for selection by an end user, and so a list of Holders are returned instead of a list of
	 * definitions so that the system knows which provider the definition came from
	 *
	 * @return a list of CohortDefinitionItemHolder defined
	 * @throws APIException
	 */
	public List<CohortDefinitionItemHolder> getAllCohortDefinitions() throws APIException;

	public List<CohortDefinitionItemHolder> getCohortDefinitions(Class<? extends CohortDefinitionProvider> providerClass)
			throws APIException;

	public CohortDefinition getCohortDefinition(Class<CohortDefinition> clazz, Integer id) throws APIException;

	public CohortDefinition getCohortDefinition(String cohortKey) throws APIException;

	public CohortDefinition saveCohortDefinition(CohortDefinition definition) throws APIException;

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
	public Cohort evaluate(CohortDefinition definition, EvaluationContext evalContext) throws APIException;

	public CohortDefinition getAllPatientsCohortDefinition() throws APIException;

}
