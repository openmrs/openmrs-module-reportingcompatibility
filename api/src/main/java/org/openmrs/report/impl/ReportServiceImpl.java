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
package org.openmrs.report.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.cohort.Cohort;
import org.openmrs.Concept;
import org.openmrs.ConceptSet;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.GlobalProperty;
import org.openmrs.Location;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.reportingcompatibility.service.DataSetService;
import org.openmrs.module.reportingcompatibility.service.ReportService;
import org.openmrs.report.DataSet;
import org.openmrs.report.DataSetDefinition;
import org.openmrs.report.EvaluationContext;
import org.openmrs.report.RenderingMode;
import org.openmrs.report.ReportConstants;
import org.openmrs.report.ReportData;
import org.openmrs.report.ReportRenderer;
import org.openmrs.report.ReportSchema;
import org.openmrs.report.ReportSchemaXml;
import org.openmrs.report.db.ReportDAO;
import org.openmrs.util.OpenmrsClassLoader;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.transaction.annotation.Transactional;

/**
 * Methods specific to objects in the report package. These methods render reports or save them to
 * the database
 *
 * @see org.openmrs.module.reportingcompatibility.service.ReportService
 * @see org.openmrs.api.context.Context
 */
@Transactional
public class ReportServiceImpl implements ReportService {
	
	public Log log = LogFactory.getLog(this.getClass());
	
	private ReportDAO dao = null;
	
	/**
	 * Report renderers that have been registered. This is filled via {@link #setRenderers(Map)} and
	 * spring's applicationContext-service.xml object
	 */
	private static Map<Class<? extends ReportRenderer>, ReportRenderer> renderers = null;
	
	/**
	 * Default constructor
	 */
	public ReportServiceImpl() {
	}
	
	/**
	 * Method used by Spring injection to set the ReportDAO implementation to use in this service
	 *
	 * @param dao The ReportDAO to use in this service
	 */
	public void setReportDAO(ReportDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * Clean up after this class. Set the static var to null so that the classloader can reclaim the
	 * space.
	 *
	 * @see org.openmrs.api.impl.BaseOpenmrsService#onShutdown()
	 */
	public void onShutdown() {
		setRenderers(null);
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#deleteReportSchema(org.openmrs.report.ReportSchema)
	 */
	public void deleteReportSchema(ReportSchema reportSchema) {
		throw new APIException("Not Yet Implemented");
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#evaluate(org.openmrs.report.ReportSchema,
	 *      org.openmrs.Cohort, org.openmrs.report.EvaluationContext)
	 */
	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true)
	public ReportData evaluate(ReportSchema reportSchema, Cohort inputCohort, EvaluationContext evalContext) {
		ReportData ret = new ReportData();
		Map<String, DataSet> data = new HashMap<String, DataSet>();
		ret.setDataSets(data);
		ret.setReportSchema(reportSchema);
		ret.setEvaluationContext(evalContext);
		DataSetService dss = Context.getService(DataSetService.class);
		
		if (reportSchema.getDataSetDefinitions() != null) {
			for (DataSetDefinition dataSetDefinition : reportSchema.getDataSetDefinitions()) {
				data.put(dataSetDefinition.getName(), dss.evaluate(dataSetDefinition, inputCohort, evalContext));
			}
		}
		
		return ret;
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportRenderer(java.lang.String)
	 */
	@Transactional(readOnly = true)
	public ReportRenderer getReportRenderer(Class<? extends ReportRenderer> clazz) {
		try {
			return renderers.get(clazz);
		}
		catch (Exception ex) {
			log.error("Failed to get report renderer for " + clazz, ex);
			return null;
		}
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportRenderer(java.lang.String)
	 */
	@Transactional(readOnly = true)
	public ReportRenderer getReportRenderer(String className) {
		try {
			return renderers.get(OpenmrsClassLoader.getInstance().loadClass(className));
		}
		catch (Exception ex) {
			log.error("Failed to get report renderer for " + className, ex);
			return null;
		}
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportRenderers()
	 */
	@Transactional(readOnly = true)
	public Collection<ReportRenderer> getReportRenderers() {
		return getRenderers().values();
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getRenderingModes(org.openmrs.report.ReportSchema)
	 */
	@Transactional(readOnly = true)
	public List<RenderingMode> getRenderingModes(ReportSchema schema) {
		List<RenderingMode> ret = new Vector<RenderingMode>();
		for (ReportRenderer r : getReportRenderers()) {
			Collection<RenderingMode> modes = r.getRenderingModes(schema);
			if (modes != null) {
				ret.addAll(modes);
			}
		}
		Collections.sort(ret);
		return ret;
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportSchema(java.lang.Integer)
	 */
	@Transactional(readOnly = true)
	public ReportSchema getReportSchema(Integer reportSchemaId) throws APIException {
		ReportSchemaXml xml = getReportSchemaXml(reportSchemaId);
		return getReportSchema(xml);
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportSchema(org.openmrs.report.ReportSchemaXml)
	 */
	@Transactional(readOnly = true)
	public ReportSchema getReportSchema(ReportSchemaXml reportSchemaXml) throws APIException {
		ReportSchema reportSchema = null;
		if (reportSchemaXml == null || reportSchemaXml.getXml() == null || reportSchemaXml.getXml().length() == 0) {
			throw new APIException("The current serialized ReportSchema string named 'xml' is null or empty");
		}
		String expandedXml = applyReportXmlMacros(reportSchemaXml.getXml());
		try {
			reportSchema = Context.getSerializationService().getDefaultSerializer().deserialize(expandedXml, ReportSchema.class);
		}
		catch (Exception e) {
			throw new APIException(e);
		}
		return reportSchema;
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportSchemas()
	 */
	@Transactional(readOnly = true)
	public List<ReportSchema> getReportSchemas() throws APIException {
		List<ReportSchema> ret = new ArrayList<ReportSchema>();
		for (ReportSchemaXml xml : getReportSchemaXmls()) {
			ret.add(getReportSchema(xml));
		}
		return ret;
	}
	
	/**
	 * ADDs renderers...doesn't replace them.
	 *
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#setRenderers(java.util.Map)
	 */
	public void setRenderers(Map<Class<? extends ReportRenderer>, ReportRenderer> newRenderers) throws APIException {
		for (Map.Entry<Class<? extends ReportRenderer>, ReportRenderer> entry : newRenderers.entrySet()) {
			registerRenderer(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getRenderers()
	 */
	@Transactional(readOnly = true)
	public Map<Class<? extends ReportRenderer>, ReportRenderer> getRenderers() throws APIException {
		if (renderers == null) {
			renderers = new LinkedHashMap<Class<? extends ReportRenderer>, ReportRenderer>();
		}
		
		return renderers;
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#registerRenderer(java.lang.Class,
	 *      org.openmrs.report.ReportRenderer)
	 */
	public void registerRenderer(Class<? extends ReportRenderer> rendererClass, ReportRenderer renderer) throws APIException {
		getRenderers().put(rendererClass, renderer);
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#registerRenderer(java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public void registerRenderer(String rendererClass) throws APIException {
		try {
			Class loadedClass = OpenmrsClassLoader.getInstance().loadClass(rendererClass);
			registerRenderer(loadedClass, (ReportRenderer) loadedClass.newInstance());
			
		}
		catch (Exception e) {
			throw new APIException("Unable to load and instantiate renderer", e);
		}
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#removeRenderer(Class)
	 */
	public void removeRenderer(Class<? extends ReportRenderer> renderingClass) {
		renderers.remove(renderingClass);
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#saveReportSchema(org.openmrs.report.ReportSchema)
	 */
	public void saveReportSchema(ReportSchema reportSchema) {
		throw new APIException("Not Yet Implemented");
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportSchemaXml(java.lang.Integer)
	 */
	@Transactional(readOnly = true)
	public ReportSchemaXml getReportSchemaXml(Integer reportSchemaXmlId) {
		return dao.getReportSchemaXml(reportSchemaXmlId);
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#saveReportSchemaXml(org.openmrs.report.ReportSchemaXml)
	 */
	public void saveReportSchemaXml(ReportSchemaXml reportSchemaXml) {
		dao.saveReportSchemaXml(reportSchemaXml);
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#createReportSchemaXml(org.openmrs.report.ReportSchemaXml)
	 * @deprecated use saveReportSchemaXml(reportSchemaXml)
	 */
	@Deprecated
	public void createReportSchemaXml(ReportSchemaXml reportSchemaXml) {
		Context.getService(ReportService.class).saveReportSchemaXml(reportSchemaXml);
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#updateReportSchemaXml(org.openmrs.report.ReportSchemaXml)
	 * @deprecated use saveReportSchemaXml(reportSchemaXml)
	 */
	@Deprecated
	public void updateReportSchemaXml(ReportSchemaXml reportSchemaXml) {
		Context.getService(ReportService.class).saveReportSchemaXml(reportSchemaXml);
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#deleteReportSchemaXml(org.openmrs.report.ReportSchemaXml)
	 */
	public void deleteReportSchemaXml(ReportSchemaXml reportSchemaXml) {
		dao.deleteReportSchemaXml(reportSchemaXml);
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportSchemaXmls()
	 */
	@Transactional(readOnly = true)
	public List<ReportSchemaXml> getReportSchemaXmls() {
		return dao.getReportSchemaXmls();
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#getReportXmlMacros()
	 */
	@Transactional(readOnly = true)
	public Properties getReportXmlMacros() {
		try {
			String macrosAsString = Context.getAdministrationService().getGlobalProperty(
			    ReportConstants.GLOBAL_PROPERTY_REPORT_XML_MACROS);
			Properties macros = new Properties();
			if (macrosAsString != null) {
				OpenmrsUtil.loadProperties(macros, new ByteArrayInputStream(macrosAsString.getBytes("UTF-8")));
			}
			return macros;
		}
		catch (Exception ex) {
			throw new APIException(ex);
		}
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#saveReportXmlMacros(java.util.Properties)
	 */
	public void saveReportXmlMacros(Properties macros) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OpenmrsUtil.storeProperties(macros, out, null);
			GlobalProperty prop = new GlobalProperty(ReportConstants.GLOBAL_PROPERTY_REPORT_XML_MACROS, out.toString());
			Context.getAdministrationService().saveGlobalProperty(prop);
		}
		catch (Exception ex) {
			throw new APIException(ex);
		}
	}
	
	/**
	 * @see org.openmrs.module.reportingcompatibility.service.ReportService#applyReportXmlMacros(java.lang.String)
	 */
	@Transactional(readOnly = true)
	public String applyReportXmlMacros(String input) {
		Properties macros = getReportXmlMacros();
		if (macros != null && macros.size() > 0) {
			log.debug("XML Before macros: " + input);
			String prefix = macros.getProperty("macroPrefix", "");
			String suffix = macros.getProperty("macroSuffix", "");
			while (true) {
				String replacement = input;
				for (Map.Entry<Object, Object> e : macros.entrySet()) {
					String key = prefix + e.getKey() + suffix;
					String value = e.getValue() == null ? "" : e.getValue().toString();
					log.debug("Trying to replace " + key + " with " + value);
					replacement = replacement.replace(key, (String) e.getValue());
				}
				if (input.equals(replacement)) {
					log.debug("Macro expansion complete.");
					break;
				}
				input = replacement;
				log.debug("XML Exploded to: " + input);
			}
		}
		return input;
	}
	
	public Cohort getAllPatients() throws DAOException {
		return dao.getAllPatients();
	}
	
	public Cohort getInverseOfCohort(Cohort cohort) {
		// TODO see if this can be sped up by delegating to the database
		return Cohort.subtract(getAllPatients(), cohort);
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
		Map<Integer, Collection<Integer>> activeDrugs = dao.getActiveDrugIds(patientIds, onDate, onDate);
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
		
		Map<Integer, Collection<Integer>> activeDrugs = dao.getActiveDrugIds(patientIds, fromDate, toDate);
		Set<Integer> ret = new HashSet<Integer>();
		
		if (drugIds == null) {
			drugIds = new ArrayList<Integer>();
		}
		
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
				if (patientIds == null) {
					patientIds = getAllPatients().getMemberIds();
				}
				// first get all patients taking no drugs at all
				ret.addAll(patientIds);
				ret.removeAll(activeDrugs.keySet());
				
				// next get all patients taking drugs, but not the specified ones
				for (Map.Entry<Integer, Collection<Integer>> e : activeDrugs.entrySet()) {
					if (!OpenmrsUtil.containsAny(e.getValue(), drugIds)) {
						ret.add(e.getKey());
					}
				}
				
			} else if (groupMethod == GroupMethod.ALL) {
				// Patients taking all of the specified drugs
				for (Map.Entry<Integer, Collection<Integer>> e : activeDrugs.entrySet()) {
					if (e.getValue().containsAll(drugIds)) {
						ret.add(e.getKey());
					}
				}
				
			} else { // groupMethod == GroupMethod.ANY
				// Patients taking any of the specified drugs
				for (Map.Entry<Integer, Collection<Integer>> e : activeDrugs.entrySet()) {
					if (OpenmrsUtil.containsAny(e.getValue(), drugIds)) {
						ret.add(e.getKey());
					}
				}
			}
		}
		Cohort ps = new Cohort("Cohort from drug orders", "", ret);
		return ps;
	}
	
	public Cohort getPatientsHavingDrugOrder(List<Drug> drug, List<Concept> drugConcept, Date startDateFrom,
	        Date startDateTo, Date stopDateFrom, Date stopDateTo, List<Concept> discontinuedReason) {
		return dao.getPatientsHavingDrugOrder(drug, drugConcept, startDateFrom, startDateTo, stopDateFrom,
		    stopDateTo, discontinuedReason);
	}
	
	public Cohort getPatientsHavingEncounters(EncounterType encounterType, Location location, Form form, Date fromDate,
	                                          Date toDate, Integer minCount, Integer maxCount) {
		List<EncounterType> list = encounterType == null ? null : Collections.singletonList(encounterType);
		return dao.getPatientsHavingEncounters(list, location, form, fromDate, toDate, minCount, maxCount);
	}
	
	public Cohort getPatientsHavingEncounters(List<EncounterType> encounterTypeList, Location location, Form form,
	                                          Date fromDate, Date toDate, Integer minCount, Integer maxCount) {
		return dao.getPatientsHavingEncounters(encounterTypeList, location, form, fromDate, toDate, minCount,
		    maxCount);
	}
	
	public Cohort getPatientsHavingLocation(Location loc) {
		return dao.getPatientsHavingLocation(loc.getLocationId(), PatientLocationMethod.PATIENT_HEALTH_CENTER);
	}
	
	public Cohort getPatientsHavingLocation(Location loc, PatientLocationMethod method) {
		return dao.getPatientsHavingLocation(loc.getLocationId(), method);
	}
	
	public Cohort getPatientsHavingLocation(Integer locationId) {
		return dao.getPatientsHavingLocation(locationId, PatientLocationMethod.PATIENT_HEALTH_CENTER);
	}
	
	public Cohort getPatientsHavingLocation(Integer locationId, PatientLocationMethod method) {
		return dao.getPatientsHavingLocation(locationId, method);
	}
	
	public Cohort getPatientsHavingObs(Integer conceptId, TimeModifier timeModifier,
	                                   Modifier modifier, Object value, Date fromDate, Date toDate) {
		return dao.getPatientsHavingObs(conceptId, timeModifier, modifier, value, fromDate, toDate);
	}
	
	/**
	 * @see org.openmrs.api.PatientSetService#getPatientsByCharacteristics(java.lang.String, java.util.Date, java.util.Date)
	 * @return cohort of patients given gender and birth date range
	 * <strong>Should</strong> return cohort that contains patients with given gender and birth date range
	 */
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate) throws DAOException {
		return getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, null, null, null, null);
	}
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate) throws DAOException {
		return getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minDeathdate, maxDeathdate, null, null, null, null, null);
	}
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate, Integer minAge,
	        Integer maxAge, Boolean aliveOnly, Boolean deadOnly) throws DAOException {
		return dao.getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minDeathdate, maxDeathdate, minAge, maxAge,
		    aliveOnly, deadOnly);
	}
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly) throws DAOException {
		return dao.getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minBirthdate, maxBirthdate, minAge, maxAge,
				aliveOnly, deadOnly);
	}
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Integer minAge,
	        Integer maxAge, Boolean aliveOnly, Boolean deadOnly, Date effectiveDate) throws DAOException {
		return dao.getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minAge, maxAge,
		    aliveOnly, deadOnly, effectiveDate);
	}
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate , Date minDeathdate, Date maxDeathdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly, Date effectiveDate) throws DAOException {
		return dao.getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minDeathdate, maxDeathdate, minAge, maxAge,
				aliveOnly, deadOnly, effectiveDate);
	}
	
	/**
	 * @see org.openmrs.api.PatientSetService#getPatientsHavingPersonAttribute(org.openmrs.PersonAttributeType, java.lang.String)
	 * @return cohort that contains patients given person attribute type and value
	 * <strong>Should</strong> return cohort that contains patients given person attribute type and value
	 */
	public Cohort getPatientsHavingPersonAttribute(PersonAttributeType attribute, String value) {
		return dao.getPatientsHavingPersonAttribute(attribute, value);
	}
	
	/**
	 * @see org.openmrs.api.PatientSetService#getPatientsInProgram(org.openmrs.Program, java.util.Date, java.util.Date)
	 * @return cohort of patients currently in the program within the date range
	 * <strong>Should</strong> get cohort of patients currently in the program with the date range
	 */
	public Cohort getPatientsInProgram(Program program, Date fromDate, Date toDate) {
		return dao.getPatientsInProgram(program.getProgramId(), fromDate, toDate);
	}
	
	public Cohort getPatientsByProgramAndState(Program program, List<ProgramWorkflowState> stateList, Date fromDate, Date toDate) {
		return dao.getPatientsByProgramAndState(program, stateList, fromDate, toDate);
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
		return dao.getCurrentDrugOrders(ps, drugConcepts);
	}
	
	public Map<Integer, List<Relationship>> getRelationships(Cohort ps, RelationshipType relType) {
		return dao.getRelationships(ps, relType);
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
		return dao.getDrugOrders(ps, drugConcepts);
	}
	
	public Map<Integer, Encounter> getFirstEncountersByType(Cohort patients, EncounterType encType) {
		List<EncounterType> types = new Vector<EncounterType>();
		if (encType != null) {
			types.add(encType);
		}
		return dao.getFirstEncountersByType(patients, types);
	}
	
	public Map<Integer, Object> getFirstEncounterAttrsByType(Cohort patients, List<EncounterType> encTypes, String attr) {
		if (encTypes == null) {
			encTypes = new Vector<EncounterType>();
		}
		
		return dao.getEncounterAttrsByType(patients, encTypes, attr, true);
	}
	
	public Map<Integer, Encounter> getFirstEncountersByType(Cohort patients, List<EncounterType> types) {
		return dao.getFirstEncountersByType(patients, types);
	}
	
	public Map<Integer, Encounter> getEncountersByType(Cohort patients, EncounterType encType) {
		List<EncounterType> types = new Vector<EncounterType>();
		if (encType != null) {
			types.add(encType);
		}
		return dao.getEncountersByType(patients, types);
	}
	
	public Map<Integer, Encounter> getEncountersByType(Cohort patients, List<EncounterType> types) {
		return dao.getEncountersByType(patients, types);
	}
	
	public Map<Integer, Object> getEncounterAttrsByType(Cohort patients, List<EncounterType> encTypes, String attr) {
		if (encTypes == null) {
			encTypes = new Vector<EncounterType>();
		}
		
		return dao.getEncounterAttrsByType(patients, encTypes, attr, false);
	}
	
	public Map<Integer, List<List<Object>>> getObservationsValues(Cohort patients, Concept c) {
		return getObservationsValues(patients, c, null, null, true);
	}
	
	public Map<Integer, List<List<Object>>> getObservationsValues(Cohort patients, Concept c, List<String> attributes,
	        Integer limit, boolean showMostRecentFirst) {
		if (attributes == null) {
			attributes = new Vector<String>();
		}
		
		// add null for the actual obs value
		if (attributes.size() < 1 || attributes.get(0) != null) {
			attributes.add(0, null);
		}
		
		return dao.getObservationsValues(patients, c, attributes, limit, showMostRecentFirst);
	}
	
	/**
	 * @see org.openmrs.api.PatientSetService#getPatientAttributes(Cohort, String, String, boolean)
	 */
	public Map<Integer, Object> getPatientAttributes(Cohort patients, String className, String property, boolean returnAll) {
		return dao.getPatientAttributes(patients, className, property, returnAll);
	}
	
	public Map<Integer, Object> getPatientAttributes(Cohort patients, String classNameDotProperty, boolean returnAll) {
		String[] temp = classNameDotProperty.split("\\.");
		if (temp.length != 2) {
			throw new IllegalArgumentException(classNameDotProperty + " must be ClassName.property");
		}
		return getPatientAttributes(patients, temp[0], temp[1], returnAll);
	}
	
	public Map<Integer, String> getPatientIdentifierStringsByType(Cohort patients, PatientIdentifierType type) {
		List<PatientIdentifierType> types = new Vector<PatientIdentifierType>();
		if (type != null) {
			types.add(type);
		}
		return dao.getPatientIdentifierByType(patients, types);
	}
	
	/**
	 * @see org.openmrs.api.PatientSetService#getPersonAttributes(org.openmrs.Cohort,
	 *      java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	public Map<Integer, Object> getPersonAttributes(Cohort patients, String attributeName, String joinClass,
	        String joinProperty, String outputColumn, boolean returnAll) {
		return dao.getPersonAttributes(patients, attributeName, joinClass, joinProperty, outputColumn,
		    returnAll);
	}
	
	public Map<Integer, PatientProgram> getPatientPrograms(Cohort ps, Program program) {
		return dao.getPatientPrograms(ps, program, false, true);
	}
}
