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
package org.openmrs.module.reportingcompatibility.service.db;

import org.openmrs.*;
import org.openmrs.api.PatientSetService;
import org.openmrs.api.PatientSetService.PatientLocationMethod;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.db.DAOException;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ReportingCompatibilityDAO {
	
	public Cohort getAllPatients();
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate,Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly) throws DAOException;
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate,Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly, Date effectiveDate)
	                                                                                                                   throws DAOException;
	
	public Cohort getPatientsHavingDateObs(Integer conceptId, Date startTime, Date endTime);
	
	public Cohort getPatientsHavingNumericObs(Integer conceptId, TimeModifier timeModifier,
	                                          PatientSetService.Modifier modifier, Number value, Date fromDate, Date toDate)
	                                                                                                                        throws DAOException;
	
	public Cohort getPatientsHavingObs(Integer conceptId, TimeModifier timeModifier, PatientSetService.Modifier modifier,
	                                   Object value, Date fromDate, Date toDate) throws DAOException;
	
	public Cohort getPatientsHavingEncounters(List<EncounterType> encounterTypeList, Location location, Form form,
	                                          Date fromDate, Date toDate, Integer minCount, Integer maxCount)
	                                                                                                         throws DAOException;
	
	public Cohort getPatientsByProgramAndState(Program program, List<ProgramWorkflowState> stateList, Date fromDate,
	                                           Date toDate) throws DAOException;
	
	public Cohort getPatientsInProgram(Integer programId, Date fromDate, Date toDate) throws DAOException;
	
	public Cohort getPatientsHavingTextObs(Integer conceptId, String value, TimeModifier timeModifier) throws DAOException;
	
	public Cohort getPatientsHavingLocation(Integer locationId, PatientLocationMethod method) throws DAOException;
	
	public Cohort getPatientsBySqlQuery(String sqlQuery) throws DAOException;
	
	public Map<Integer, String> getShortPatientDescriptions(Collection<Integer> patientIds) throws DAOException;
	
	public Map<Integer, List<Obs>> getObservations(Cohort patients, Concept concept, Date fromDate, Date toDate)
	                                                                                                            throws DAOException;
	
	public Map<Integer, List<List<Object>>> getObservationsValues(Cohort patients, Concept c, List<String> attributes, Integer limit, boolean showMostRecentFirst);
	
	public Map<Integer, Encounter> getEncountersByType(Cohort patients, List<EncounterType> encType);
	
	public Map<Integer, Object> getEncounterAttrsByType(Cohort patients, List<EncounterType> encTypes, String attr,
	                                                    Boolean earliestFirst);
	
	public Map<Integer, Encounter> getEncounters(Cohort patients);
	
	public Map<Integer, Encounter> getFirstEncountersByType(Cohort patients, List<EncounterType> encType);
	
	public Map<Integer, Object> getPatientAttributes(Cohort patients, String className, String property, boolean returnAll);
	
	public Map<Integer, String> getPatientIdentifierByType(Cohort patients, List<PatientIdentifierType> types);
	
	public Map<Integer, Map<String, Object>> getCharacteristics(Cohort patients) throws DAOException;
	
	public Cohort convertPatientIdentifier(List<String> identifiers) throws DAOException;
	
	public List<Patient> getPatients(Collection<Integer> patientIds) throws DAOException;
	
	public Map<Integer, Collection<Integer>> getActiveDrugIds(Collection<Integer> patientIds, Date fromDate, Date toDate)
	                                                                                                                     throws DAOException;
	
	public Map<Integer, PatientState> getCurrentStates(Cohort ps, ProgramWorkflow wf) throws DAOException;
	
	public Map<Integer, PatientProgram> getPatientPrograms(Cohort ps, Program program, boolean includeVoided,
	                                                       boolean includePast) throws DAOException;
	
	public Map<Integer, List<DrugOrder>> getCurrentDrugOrders(Cohort ps, List<Concept> drugConcepts) throws DAOException;
	
	public Map<Integer, List<DrugOrder>> getDrugOrders(Cohort ps, List<Concept> drugConcepts) throws DAOException;
	
	public Map<Integer, List<Relationship>> getRelationships(Cohort ps, RelationshipType relType) throws DAOException;
	
	public Map<Integer, List<Person>> getRelatives(Cohort ps, RelationshipType relType, boolean forwards)
	                                                                                                     throws DAOException;
	
	public Map<Integer, Object> getPersonAttributes(Cohort patients, String attributeName, String joinClass,
	                                                String joinProperty, String outputColumn, boolean returnAll);
	
	public Cohort getPatientsHavingPersonAttribute(PersonAttributeType attribute, String value);
	
	public Cohort getPatientsHavingDrugOrder(List<Drug> drugList, List<Concept> drugConceptList, Date startDateFrom,
	                                         Date startDateTo, Date stopDateFrom, Date stopDateTo, List<Concept> discontinuedReason);
	
	public List<Encounter> getEncountersByForm(Cohort patients, List<Form> forms);
	
	/**
	 * Returns the cohort of patients matching a particular relationship search.
	 * If relType is specified, then search for patients at one or either end of that relationship type.
	 * (Which end is controlled by includeAtoB and includeBtoA.)
	 * If target is specified, then that person must be at the other end of the relationship. 
	 * 
	 * @param relType
	 * @param includeAtoB
	 * @param includeBtoA
	 * @param target
	 * @return patients matching the specified relationship search
	 */
	public Cohort getPatientsByRelationship(RelationshipType relType, boolean includeAtoB, boolean includeBtoA, Person target);

	public Integer getCountOfPatients();

	public Cohort getPatients(Integer start, Integer size);

}
