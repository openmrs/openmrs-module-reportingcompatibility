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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptSet;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.Person;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.PatientSetService;
import org.openmrs.api.PatientSetService.GroupMethod;
import org.openmrs.api.PatientSetService.PatientLocationMethod;
import org.openmrs.api.PatientSetService.TimeModifier;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.api.impl.PatientSetServiceImpl;
import org.openmrs.module.reportingcompatibility.service.db.ReportingCompatibilityDAO;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class ReportingCompatibilityServiceImpl extends BaseOpenmrsService implements ReportingCompatibilityService {
	
	public final Log log = LogFactory.getLog(this.getClass());
	
	private ReportingCompatibilityDAO dao;
	
	public ReportingCompatibilityServiceImpl() {
	}
	
	private ReportingCompatibilityDAO getDao() {
		if (!Context.hasPrivilege(OpenmrsConstants.PRIV_VIEW_PATIENT_COHORTS)) {
			throw new APIAuthenticationException("Privilege required: " + OpenmrsConstants.PRIV_VIEW_PATIENT_COHORTS);
		}
		return dao;
	}
	
	public void setDao(ReportingCompatibilityDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * Clean up after this class. Set the static var to null so that the classloader can reclaim the
	 * space.
	 * 
	 * @see org.openmrs.api.impl.BaseOpenmrsService#onShutdown()
	 */
	public void onShutdown() {
	}
	
	public Cohort getAllPatients() throws DAOException {
		return getDao().getAllPatients();
	}
	
    public Cohort getInverseOfCohort(Cohort cohort) {
	    // TODO see if this can be sped up by delegating to the database
		return Cohort.subtract(getAllPatients(), cohort);
    }
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate) throws DAOException {
		return getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minDeathdate, maxDeathdate,  null, null, null, null);
	}
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate,Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly) throws DAOException {
		return getDao().getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minDeathdate, maxDeathdate, minAge, maxAge,
		    aliveOnly, deadOnly);
	}
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly, Date effectiveDate)
	                                                                                                                   throws DAOException {
		return getDao().getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minDeathdate, maxDeathdate, minAge, maxAge,
		    aliveOnly, deadOnly, effectiveDate);
	}
	
	public Cohort getPatientsHavingDateObs(Integer conceptId, Date startTime, Date endTime) {
		return getDao().getPatientsHavingDateObs(conceptId, startTime, endTime);
	}
	
	public Cohort getPatientsHavingNumericObs(Integer conceptId, PatientSetService.TimeModifier timeModifier,
	                                          PatientSetServiceImpl.Modifier modifier, Number value, Date fromDate,
	                                          Date toDate) {
		return getDao().getPatientsHavingNumericObs(conceptId, timeModifier, modifier, value, fromDate, toDate);
	}
	
	public Cohort getPatientsHavingObs(Integer conceptId, TimeModifier timeModifier,
	                                   PatientSetServiceImpl.Modifier modifier, Object value, Date fromDate, Date toDate) {
		return getDao().getPatientsHavingObs(conceptId, timeModifier, modifier, value, fromDate, toDate);
	}
	
	public Cohort getPatientsHavingEncounters(EncounterType encounterType, Location location, Form form, Date fromDate,
											  Date toDate, Integer minCount, Integer maxCount) {
		List<EncounterType> list = encounterType == null ? null : Collections.singletonList(encounterType);
		return getDao().getPatientsHavingEncounters(list, location, form, fromDate, toDate, minCount, maxCount);
	}
	
	public Cohort getPatientsHavingEncounters(List<EncounterType> encounterTypeList, Location location, Form form,
	                                          Date fromDate, Date toDate, Integer minCount, Integer maxCount) {
		return getDao().getPatientsHavingEncounters(encounterTypeList, location, form, fromDate, toDate, minCount,
		    maxCount);
	}
	
	public Cohort getPatientsByProgramAndState(Program program, List<ProgramWorkflowState> stateList, Date fromDate,
											   Date toDate) {
		return getDao().getPatientsByProgramAndState(program, stateList, fromDate, toDate);
	}
	
	public Cohort getPatientsInProgram(Program program, Date fromDate, Date toDate) {
		return getDao().getPatientsInProgram(program.getProgramId(), fromDate, toDate);
	}
	
	public Cohort getPatientsHavingTextObs(Concept concept, String value, TimeModifier timeModifier) {
		return getPatientsHavingTextObs(concept.getConceptId(), value, timeModifier);
	}
	
	public Cohort getPatientsHavingTextObs(Integer conceptId, String value, TimeModifier timeModifier) {
		return getDao().getPatientsHavingTextObs(conceptId, value, timeModifier);
	}
	
	public Cohort getPatientsHavingLocation(Location loc) {
		return getPatientsHavingLocation(loc.getLocationId(), PatientLocationMethod.PATIENT_HEALTH_CENTER);
	}
	
	public Cohort getPatientsHavingLocation(Location loc, PatientLocationMethod method) {
		return getPatientsHavingLocation(loc.getLocationId(), method);
	}
	
	public Cohort getPatientsHavingLocation(Integer locationId) {
		return getPatientsHavingLocation(locationId, PatientLocationMethod.PATIENT_HEALTH_CENTER);
	}
	
	public Cohort getPatientsHavingLocation(Integer locationId, PatientLocationMethod method) {
		return getDao().getPatientsHavingLocation(locationId, method);
	}
	
	/**
	 * Returns a PatientSet of patient who had drug orders for a set of drugs active on a certain
	 * date. Can also be used to find patient with no drug orders on that date.
	 * 
	 * @param patientIds Collection of patientIds you're interested in. NULL means all patients.
	 * @param takingIds Collection of drugIds the patient is taking. (Or the empty set to mean
	 *            "any drug" or NULL to mean "no drugs")
	 * @param onDate Which date to look at the patients' drug orders. (NULL defaults to now().)
	 * @return Cohort of Patients matching criteria
	 */
	public Cohort getPatientsHavingDrugOrder(Collection<Integer> patientIds, Collection<Integer> takingIds, Date onDate) {
		Map<Integer, Collection<Integer>> activeDrugs = getDao().getActiveDrugIds(patientIds, onDate, onDate);
		Set<Integer> ret = new HashSet<Integer>();
		boolean takingAny = takingIds != null && takingIds.size() == 0;
		boolean takingNone = takingIds == null;
		if (takingAny) {
			ret.addAll(activeDrugs.keySet());
		} else if (takingNone) {
			if (patientIds == null) {
				patientIds = getAllPatients().getMemberIds();
			}
			patientIds.removeAll(activeDrugs.keySet());
			ret.addAll(patientIds);
		} else { // taking any of the drugs in takingIds
			for (Map.Entry<Integer, Collection<Integer>> e : activeDrugs.entrySet()) {
				for (Integer drugId : takingIds) {
					if (e.getValue().contains(drugId)) {
						ret.add(e.getKey());
						break;
					}
				}
			}
		}
		return new Cohort("Cohort from drug orders", "", ret);
	}
	
	public Cohort getPatientsHavingDrugOrder(Collection<Integer> patientIds, Collection<Integer> drugIds,
	                                         GroupMethod groupMethod, Date fromDate, Date toDate) {
		
		Map<Integer, Collection<Integer>> activeDrugs = getDao().getActiveDrugIds(patientIds, fromDate, toDate);
		Set<Integer> ret = new HashSet<Integer>();
		
		if (drugIds == null)
			drugIds = new ArrayList<Integer>();
		
		if (drugIds.size() == 0) {
			if (groupMethod == GroupMethod.NONE) {
				// Patients taking no drugs
				if (patientIds == null) {
					patientIds = getAllPatients().getMemberIds();
				}
				patientIds.removeAll(activeDrugs.keySet());
				ret.addAll(patientIds);
			} else {
				// Patients taking any drugs
				ret.addAll(activeDrugs.keySet());
			}
			
		} else {
			if (groupMethod == GroupMethod.NONE) {
				// Patients taking none of the specified drugs
				
				// first get all patients taking no drugs at all
				ret.addAll(patientIds);
				ret.removeAll(activeDrugs.keySet());
				
				// next get all patients taking drugs, but not the specified ones
				for (Map.Entry<Integer, Collection<Integer>> e : activeDrugs.entrySet())
					if (!OpenmrsUtil.containsAny(e.getValue(), drugIds))
						ret.add(e.getKey());
				
			} else if (groupMethod == GroupMethod.ALL) {
				// Patients taking all of the specified drugs
				for (Map.Entry<Integer, Collection<Integer>> e : activeDrugs.entrySet())
					if (e.getValue().containsAll(drugIds))
						ret.add(e.getKey());
				
			} else { // groupMethod == GroupMethod.ANY
				// Patients taking any of the specified drugs
				for (Map.Entry<Integer, Collection<Integer>> e : activeDrugs.entrySet())
					if (OpenmrsUtil.containsAny(e.getValue(), drugIds))
						ret.add(e.getKey());
			}
		}
		Cohort ps = new Cohort("Cohort from drug orders", "", ret);
		return ps;
	}
	
	public Cohort getPatientsHavingDrugOrder(List<Drug> drug, List<Concept> drugConcept, Date startDateFrom,
											 Date startDateTo, Date stopDateFrom, Date stopDateTo, List<Concept> discontinuedReason) {
		return getDao().getPatientsHavingDrugOrder(drug, drugConcept, startDateFrom, startDateTo, stopDateFrom,
		    stopDateTo, discontinuedReason);
	}
	
	public Cohort getPatientsHavingPersonAttribute(PersonAttributeType attribute, String value) {
		return getDao().getPatientsHavingPersonAttribute(attribute, value);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.openmrs.module.reportingcompatibility.service.ReportingCompatibilityService#getPatientsBySqlQuery(java.lang.String)
	 */
	public Cohort getPatientsBySqlQuery(String sqlQuery) {
		return getDao().getPatientsBySqlQuery(sqlQuery);
	}
	
	public Map<Integer, String> getShortPatientDescriptions(Collection<Integer> patientIds) {
		return getDao().getShortPatientDescriptions(patientIds);
	}
	
	public Map<Integer, List<Obs>> getObservations(Cohort patients, Concept concept) {
		if (patients == null || patients.size() == 0)
			return new HashMap<Integer, List<Obs>>();
		return getDao().getObservations(patients, concept, null, null);
	}
	
	/**
	 * Date range is inclusive of both endpoints
	 */
	public Map<Integer, List<Obs>> getObservations(Cohort patients, Concept concept, Date fromDate, Date toDate) {
		if (patients == null || patients.size() == 0)
			return new HashMap<Integer, List<Obs>>();
		return getDao().getObservations(patients, concept, fromDate, toDate);
	}
	
	public Map<Integer, List<List<Object>>> getObservationsValues(Cohort patients, Concept c) {
		return getObservationsValues(patients, c, null);
	}
	
	@Deprecated
	public Map<Integer, List<List<Object>>> getObservationsValues(Cohort patients, Concept c, List<String> attributes) {
		return getObservationsValues(patients, c, attributes, null, true);
	}
	
	public Map<Integer, List<List<Object>>> getObservationsValues(Cohort patients, Concept c, List<String> attributes, Integer limit, boolean showMostRecentFirst) {
		if (attributes == null)
			attributes = new Vector<String>();
		
		// add null for the actual obs value
		if (attributes.size() < 1 || attributes.get(0) != null)
			attributes.add(0, null);
		
		return getDao().getObservationsValues(patients, c, attributes, limit, showMostRecentFirst);
	}
	
	public Map<Integer, Encounter> getEncountersByType(Cohort patients, EncounterType encType) {
		List<EncounterType> types = new Vector<EncounterType>();
		if (encType != null)
			types.add(encType);
		return getDao().getEncountersByType(patients, types);
	}
	
	public Map<Integer, Object> getEncounterAttrsByType(Cohort patients, List<EncounterType> encTypes, String attr) {
		if (encTypes == null)
			encTypes = new Vector<EncounterType>();
		
		return getDao().getEncounterAttrsByType(patients, encTypes, attr, false);
	}
	
	public Map<Integer, Encounter> getEncountersByType(Cohort patients, List<EncounterType> types) {
		return getDao().getEncountersByType(patients, types);
	}
	
	public Map<Integer, Encounter> getEncounters(Cohort patients) {
		return getDao().getEncounters(patients);
	}
	
	public Map<Integer, Encounter> getFirstEncountersByType(Cohort patients, EncounterType encType) {
		List<EncounterType> types = new Vector<EncounterType>();
		if (encType != null)
			types.add(encType);
		return getDao().getFirstEncountersByType(patients, types);
	}
	
	public Map<Integer, Object> getFirstEncounterAttrsByType(Cohort patients, List<EncounterType> encTypes, String attr) {
		if (encTypes == null)
			encTypes = new Vector<EncounterType>();
		
		return getDao().getEncounterAttrsByType(patients, encTypes, attr, true);
	}
	
	public Map<Integer, Encounter> getFirstEncountersByType(Cohort patients, List<EncounterType> types) {
		return getDao().getFirstEncountersByType(patients, types);
	}
	
	/**
	 * @see org.openmrs.api.PatientSetService#getPatientAttributes(Cohort, String, String, boolean)
	 */
	public Map<Integer, Object> getPatientAttributes(Cohort patients, String className, String property, boolean returnAll) {
		return getDao().getPatientAttributes(patients, className, property, returnAll);
	}
	
	public Map<Integer, Object> getPatientAttributes(Cohort patients, String classNameDotProperty, boolean returnAll) {
		String[] temp = classNameDotProperty.split("\\.");
		if (temp.length != 2) {
			throw new IllegalArgumentException(classNameDotProperty + " must be ClassName.property");
		}
		return getPatientAttributes(patients, temp[0], temp[1], returnAll);
	}
	
	public Map<Integer, PatientIdentifier> getPatientIdentifiersByType(Cohort patients, PatientIdentifierType type) {
		Map<Integer, String> strings = getPatientIdentifierStringsByType(patients, type);
		
		Map<Integer, PatientIdentifier> objects = new HashMap<Integer, PatientIdentifier>();
		for (Map.Entry<Integer, String> entry : strings.entrySet()) {
			PatientIdentifier tmpValue = new PatientIdentifier(entry.getValue(), null, null);
			objects.put(entry.getKey(), tmpValue);
		}
		return objects;
	}
	
	public Map<Integer, String> getPatientIdentifierStringsByType(Cohort patients, PatientIdentifierType type) {
		List<PatientIdentifierType> types = new Vector<PatientIdentifierType>();
		if (type != null)
			types.add(type);
		return getDao().getPatientIdentifierByType(patients, types);
	}
	
	/**
	 * @see org.openmrs.api.PatientSetService#getPersonAttributes(org.openmrs.Cohort,
	 *      java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	public Map<Integer, Object> getPersonAttributes(Cohort patients, String attributeName, String joinClass,
	                                                String joinProperty, String outputColumn, boolean returnAll) {
		return getDao().getPersonAttributes(patients, attributeName, joinClass, joinProperty, outputColumn,
		    returnAll);
	}
	
	public Map<Integer, Map<String, Object>> getCharacteristics(Cohort patients) {
		return getDao().getCharacteristics(patients);
	}
	
	public Cohort convertPatientIdentifier(List<String> identifiers) {
		return getDao().convertPatientIdentifier(identifiers);
	}
	
	public List<Patient> getPatients(Collection<Integer> patientIds) {
		return getDao().getPatients(patientIds);
	}
	
	public Map<Integer, List<Relationship>> getRelationships(Cohort ps, RelationshipType relType) {
		return getDao().getRelationships(ps, relType);
	}
	
	public Map<Integer, List<Person>> getRelatives(Cohort ps, RelationshipType relType, boolean forwards) {
		return getDao().getRelatives(ps, relType, forwards);
	}
	
	public Map<Integer, PatientState> getCurrentStates(Cohort ps, ProgramWorkflow wf) {
		return getDao().getCurrentStates(ps, wf);
	}
	
	public Map<Integer, PatientProgram> getCurrentPatientPrograms(Cohort ps, Program program) {
		return getDao().getPatientPrograms(ps, program, false, false);
	}
	
	public Map<Integer, PatientProgram> getPatientPrograms(Cohort ps, Program program) {
		return getDao().getPatientPrograms(ps, program, false, true);
	}
	
	/**
	 * @return all active drug orders whose drug concept is in the given set (or all drugs if that's
	 *         null)
	 */
	public Map<Integer, List<DrugOrder>> getCurrentDrugOrders(Cohort ps, Concept drugSet) {
		List<Concept> drugConcepts = null;
		if (drugSet != null) {
			List<ConceptSet> concepts = Context.getConceptService().getConceptSetsByConcept(drugSet);
			drugConcepts = new ArrayList<Concept>();
			for (ConceptSet cs : concepts) {
				drugConcepts.add(cs.getConcept());
			}
		}
		log.debug("drugSet: " + drugSet);
		log.debug("drugConcepts: " + drugConcepts);
		return getDao().getCurrentDrugOrders(ps, drugConcepts);
	}
	
	/**
	 * @return all drug orders whose drug concept is in the given set (or all drugs if that's null)
	 */
	public Map<Integer, List<DrugOrder>> getDrugOrders(Cohort ps, Concept drugSet) {
		List<Concept> drugConcepts = null;
		if (drugSet != null) {
			List<ConceptSet> concepts = Context.getConceptService().getConceptSetsByConcept(drugSet);
			drugConcepts = new ArrayList<Concept>();
			for (ConceptSet cs : concepts) {
				drugConcepts.add(cs.getConcept());
			}
		}
		return getDao().getDrugOrders(ps, drugConcepts);
	}
	
	/**
	 * Gets a list of encounters associated with the given form, filtered by the given patient set.
	 * 
	 * @param patients the patients to filter by (null will return all encounters for all patients)
	 * @param forms the forms to filter by
	 */
	public List<Encounter> getEncountersByForm(Cohort patients, List<Form> forms) {
		return getDao().getEncountersByForm(patients, forms);
	}
	
	public Integer getCountOfPatients() {
		return getDao().getCountOfPatients();
	}

	public Cohort getPatients(Integer start, Integer size) {
		return getDao().getPatients(start, size);
	}
}
