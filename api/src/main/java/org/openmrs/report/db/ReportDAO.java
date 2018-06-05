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
package org.openmrs.report.db;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.openmrs.cohort.Cohort;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.reportingcompatibility.service.ReportService.Modifier;
import org.openmrs.module.reportingcompatibility.service.ReportService.PatientLocationMethod;
import org.openmrs.module.reportingcompatibility.service.ReportService.TimeModifier;
import org.openmrs.report.ReportSchemaXml;

/**
 * The database methods involved with saving objects in the report package to the database
 * 
 * @see org.openmrs.module.reportingcompatibility.service.ReportService
 */
public interface ReportDAO {
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportSchemaXml(java.lang.Integer)
	 */
	public ReportSchemaXml getReportSchemaXml(Integer reportSchemaXmlId);
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#saveReportSchemaXml(org.openmrs.report.ReportSchemaXml)
	 */
	public void saveReportSchemaXml(ReportSchemaXml reportSchemaXml);
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#deleteReportSchemaXml(org.openmrs.report.ReportSchemaXml)
	 */
	public void deleteReportSchemaXml(ReportSchemaXml reportSchemaXml);
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportSchemaXmls()
	 */
	public List<ReportSchemaXml> getReportSchemaXmls();
	
	public Cohort getAllPatients();
	
	public Map<Integer, Collection<Integer>> getActiveDrugIds(Collection<Integer> patientIds, Date fromDate, Date toDate)
	        throws DAOException;
	
	public Cohort getPatientsHavingDrugOrder(List<Drug> drugList, List<Concept> drugConceptList, Date startDateFrom,
	                             	        Date startDateTo, Date stopDateFrom, Date stopDateTo, List<Concept> discontinuedReason);
	
	public Cohort getPatientsHavingEncounters(List<EncounterType> encounterTypeList, Location location, Form form,
	                              	        Date fromDate, Date toDate, Integer minCount, Integer maxCount) throws DAOException;
	
	public Cohort getPatientsHavingLocation(Integer locationId, PatientLocationMethod method) throws DAOException;
	
	public Cohort getPatientsHavingObs(Integer conceptId, TimeModifier timeModifier, Modifier modifier,
	                       	        Object value, Date fromDate, Date toDate) throws DAOException;
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly) throws DAOException;
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly, Date effectiveDate) throws DAOException;
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly) throws DAOException;
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly, Date effectiveDate) throws DAOException;
	
	public Cohort getPatientsHavingPersonAttribute(PersonAttributeType attribute, String value);
	
	public Cohort getPatientsInProgram(Integer programId, Date fromDate, Date toDate) throws DAOException;
	
	public Cohort getPatientsByProgramAndState(Program program, List<ProgramWorkflowState> stateList, Date fromDate,
	                               	        Date toDate) throws DAOException;
	
	public Map<Integer, List<DrugOrder>> getCurrentDrugOrders(Cohort ps, List<Concept> drugConcepts) throws DAOException;
	
	public Map<Integer, List<Relationship>> getRelationships(Cohort ps, RelationshipType relType) throws DAOException;
	
	public Map<Integer, List<DrugOrder>> getDrugOrders(Cohort ps, List<Concept> drugConcepts) throws DAOException;
	
	public Map<Integer, Encounter> getFirstEncountersByType(Cohort patients, List<EncounterType> encType);
	
	public Map<Integer, Object> getEncounterAttrsByType(Cohort patients, List<EncounterType> encTypes, String attr, Boolean earliestFirst);
	
	public Map<Integer, Encounter> getEncountersByType(Cohort patients, List<EncounterType> encType);
	
	public Map<Integer, List<List<Object>>> getObservationsValues(Cohort patients, Concept c, List<String> attributes,
	                                                  	        Integer limit, boolean showMostRecentFirst);
	
	public Map<Integer, Object> getPatientAttributes(Cohort patients, String className, String property, boolean returnAll);
	
	public Map<Integer, String> getPatientIdentifierByType(Cohort patients, List<PatientIdentifierType> types);
	
	public Map<Integer, Object> getPersonAttributes(Cohort patients, String attributeName, String joinClass,
	                                    	        String joinProperty, String outputColumn, boolean returnAll);
	
	public Map<Integer, PatientProgram> getPatientPrograms(Cohort ps, Program program, boolean includeVoided,
	                                           	        boolean includePast) throws DAOException;
}
