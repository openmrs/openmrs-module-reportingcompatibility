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
package org.openmrs.report.db.hibernate;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Cohort;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.Location;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.PersonAttributeType;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.Provider;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.reportingcompatibility.service.ReportService.Modifier;
import org.openmrs.module.reportingcompatibility.service.ReportService.PatientLocationMethod;
import org.openmrs.module.reportingcompatibility.service.ReportService.TimeModifier;
import org.openmrs.report.ReportSchemaXml;
import org.openmrs.report.db.ReportDAO;

/**
 * Hibernate specific database access methods for objects in the report package
 */
public class HibernateReportDAO implements ReportDAO {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	/**
	 * Hibernate session factory
	 */
	private SessionFactory sessionFactory;
	
	/**
	 * Set session factory
	 * 
	 * @param sessionFactory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	/**
	 * @see org.openmrs.report.db.ReportDAO#deleteReportSchemaXml(org.openmrs.report.ReportSchemaXml)
	 */
	public void deleteReportSchemaXml(ReportSchemaXml reportSchemaXml) {
		sessionFactory.getCurrentSession().delete(reportSchemaXml);
	}
	
	/**
	 * @see org.openmrs.report.db.ReportDAO#getReportSchemaXml(java.lang.Integer)
	 */
	public ReportSchemaXml getReportSchemaXml(Integer reportSchemaXmlId) {
		//return (ReportSchemaXml) sessionFactory.getCurrentSession()
		//                                       .get(ReportSchemaXml.class, reportSchemaXmlId);
		
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(ReportSchemaXml.class, "rsx").add(
		    Restrictions.eq("rsx.reportSchemaId", reportSchemaXmlId));
		return (ReportSchemaXml) criteria.uniqueResult();
	}
	
	/**
	 * @see org.openmrs.report.db.ReportDAO#saveReportSchemaXml(org.openmrs.report.ReportSchemaXml)
	 */
	public void saveReportSchemaXml(ReportSchemaXml reportSchemaXml) {
		sessionFactory.getCurrentSession().saveOrUpdate(reportSchemaXml);
	}
	
	/**
	 * @see org.openmrs.report.db.ReportDAO#getReportSchemaXmls()
	 */
	@SuppressWarnings("unchecked")
	public List<ReportSchemaXml> getReportSchemaXmls() {
		return sessionFactory.getCurrentSession().createQuery("from ReportSchemaXml").list();
	}
	
	@SuppressWarnings("unchecked")
	public Cohort getAllPatients() {
		
		Query query = sessionFactory.getCurrentSession().createQuery("select patientId from Patient p where p.voided = '0'");
		
		Set<Integer> ids = new HashSet<Integer>();
		ids.addAll(query.list());
		
		return new Cohort("All patients", "", ids);
	}
	
	/**
	 * Returns a Map from patientId to a Collection of drugIds for drugs active for the patients on
	 * that date If patientIds is null then do this for all patients Does not return anything for
	 * voided patients
	 * 
	 * @throws DAOException
	 */
	@SuppressWarnings("unchecked")
	public Map<Integer, Collection<Integer>> getActiveDrugIds(Collection<Integer> patientIds, Date fromDate, Date toDate)
	        throws DAOException {
		Set<Integer> idsLookup = patientIds == null ? null
		        : (patientIds instanceof HashSet ? (HashSet<Integer>) patientIds : new HashSet<Integer>(patientIds));
		
		Map<Integer, Collection<Integer>> ret = new HashMap<Integer, Collection<Integer>>();
		
		List<String> whereClauses = new ArrayList<String>();
		whereClauses.add("o.voided = false");
		if (toDate != null) {
			whereClauses.add("o.date_activated <= :toDate");
		}
		if (fromDate != null) {
			whereClauses.add("(o.auto_expire_date is null or o.auto_expire_date > :fromDate)");
			whereClauses.add("(o.date_stopped is null or o.date_stopped > :fromDate)");
		}
		
		StringBuilder sql = new StringBuilder("select o.patient_id, d.drug_inventory_id " + "from orders o "
		        + "    inner join patient p on o.patient_id = p.patient_id and p.voided = false "
		        + "    inner join drug_order d on o.order_id = d.order_id ");
		for (ListIterator<String> i = whereClauses.listIterator(); i.hasNext();) {
			sql.append((i.nextIndex() == 0 ? " where " : " and ")).append(i.next());
		}
		
		log.debug("sql= " + sql);
		
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sql.toString());
		query.setCacheMode(CacheMode.IGNORE);
		
		if (toDate != null) {
			query.setDate("toDate", toDate);
		}
		if (fromDate != null) {
			query.setDate("fromDate", fromDate);
		}
		
		List<Object[]> results = (List<Object[]>) query.list();
		for (Object[] row : results) {
			Integer patientId = (Integer) row[0];
			if (idsLookup == null || idsLookup.contains(patientId)) {
				Integer drugId = (Integer) row[1];
				Collection<Integer> drugIds = ret.get(patientId);
				if (drugIds == null) {
					drugIds = new HashSet<Integer>();
					ret.put(patientId, drugIds);
				}
				drugIds.add(drugId);
			}
		}
		return ret;
	}
	
	public Cohort getPatientsHavingDrugOrder(List<Drug> drugList, List<Concept> drugConceptList, Date startDateFrom,
	                                         Date startDateTo, Date stopDateFrom, Date stopDateTo, Boolean discontinued,
	                                         List<Concept> orderReason) {
		if (drugList != null && drugList.size() == 0) {
			drugList = null;
		}
		if (drugConceptList != null && drugConceptList.size() == 0) {
			drugConceptList = null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(" select distinct patient.id from DrugOrder where voided = false and patient.voided = false ");
		if (drugList != null) {
			sb.append(" and drug.id in (:drugIdList) ");
		}
		if (drugConceptList != null) {
			sb.append(" and concept.id in (:drugConceptIdList) ");
		}
		if (startDateFrom != null && startDateTo != null) {
			sb.append(" and dateActivated between :startDateFrom and :startDateTo ");
		} else {
			if (startDateFrom != null) {
				sb.append(" and dateActivated >= :startDateFrom ");
			}
			if (startDateTo != null) {
				sb.append(" and dateActivated <= :startDateTo ");
			}
		}
		if (orderReason != null && orderReason.size() > 0) {
			sb.append(" and orderReason.id in (:orderReasonIdList) ");
		}
		if (discontinued != null) {
			if (discontinued) {
				if (stopDateFrom != null && stopDateTo != null) {
					sb.append(" and dateStopped between :stopDateFrom and :stopDateTo ");
				} else {
					if (stopDateFrom != null) {
						sb.append(" and dateStopped >= :stopDateFrom ");
					}
					if (stopDateTo != null) {
						sb.append(" and dateStopped <= :stopDateTo ");
					}
				}
			} else { // discontinued == false
				if (stopDateFrom != null && stopDateTo != null) {
					sb.append(" and autoExpireDate between :stopDateFrom and :stopDateTo ");
				} else {
					if (stopDateFrom != null) {
						sb.append(" and autoExpireDate >= :stopDateFrom ");
					}
					if (stopDateTo != null) {
						sb.append(" and autoExpireDate <= :stopDateTo ");
					}
				}
			}
		} else { // discontinued == null, so we need either
			if (stopDateFrom != null && stopDateTo != null) {
				sb.append(" and coalesce(dateStopped, autoExpireDate) between :stopDateFrom and :stopDateTo ");
			} else {
				if (stopDateFrom != null) {
					sb.append(" and coalesce(dateStopped, autoExpireDate) >= :stopDateFrom ");
				}
				if (stopDateTo != null) {
					sb.append(" and coalesce(dateStopped, autoExpireDate) <= :stopDateTo ");
				}
			}
		}
		log.debug("sql = " + sb);
		Query query = sessionFactory.getCurrentSession().createQuery(sb.toString());
		
		if (drugList != null) {
			List<Integer> ids = new ArrayList<Integer>();
			for (Drug d : drugList) {
				ids.add(d.getDrugId());
			}
			query.setParameterList("drugIdList", ids);
		}
		if (drugConceptList != null) {
			List<Integer> ids = new ArrayList<Integer>();
			for (Concept c : drugConceptList) {
				ids.add(c.getConceptId());
			}
			query.setParameterList("drugConceptIdList", ids);
		}
		if (startDateFrom != null) {
			query.setDate("startDateFrom", startDateFrom);
		}
		if (startDateTo != null) {
			query.setDate("startDateTo", startDateTo);
		}
		if (stopDateFrom != null) {
			query.setDate("stopDateFrom", stopDateFrom);
		}
		if (stopDateTo != null) {
			query.setDate("stopDateTo", stopDateTo);
		}
		if (discontinued != null) {
			query.setBoolean("discontinued", discontinued);
		}
		if (orderReason != null && orderReason.size() > 0) {
			List<Integer> ids = new ArrayList<Integer>();
			for (Concept c : orderReason) {
				ids.add(c.getConceptId());
			}
			query.setParameterList("orderReasonIdList", ids);
		}
		
		return new Cohort(query.list());
	}
	
	/**
	 * <pre>
	 * Returns the set of patients that have encounters, with several optional parameters:
	 *   of type encounterType
	 *   at a given location
	 *   from filling out a specific form
	 *   on or after fromDate
	 *   on or before toDate
	 *   patients with at least minCount of the given encounters
	 *   patients with up to maxCount of the given encounters
	 * </pre>
	 */
	public Cohort getPatientsHavingEncounters(List<EncounterType> encounterTypeList, Location location, Form form,
	        Date fromDate, Date toDate, Integer minCount, Integer maxCount) {
		List<Integer> encTypeIds = null;
		if (encounterTypeList != null && encounterTypeList.size() > 0) {
			encTypeIds = new ArrayList<Integer>();
			for (EncounterType t : encounterTypeList) {
				encTypeIds.add(t.getEncounterTypeId());
			}
		}
		Integer locationId = location == null ? null : location.getLocationId();
		Integer formId = form == null ? null : form.getFormId();
		List<String> whereClauses = new ArrayList<String>();
		whereClauses.add("e.voided = false");
		if (encTypeIds != null) {
			whereClauses.add("e.encounter_type in (:encTypeIds)");
		}
		if (locationId != null) {
			whereClauses.add("e.location_id = :locationId");
		}
		if (formId != null) {
			whereClauses.add("e.form_id = :formId");
		}
		if (fromDate != null) {
			whereClauses.add("e.encounter_datetime >= :fromDate");
		}
		if (toDate != null) {
			whereClauses.add("e.encounter_datetime <= :toDate");
		}
		List<String> havingClauses = new ArrayList<String>();
		if (minCount != null) {
			havingClauses.add("count(*) >= :minCount");
		}
		if (maxCount != null) {
			havingClauses.add("count(*) >= :maxCount");
		}
		StringBuilder sb = new StringBuilder();
		sb.append(" select e.patient_id from encounter e ");
		sb.append(" inner join patient p on e.patient_id = p.patient_id and p.voided = false ");
		for (ListIterator<String> i = whereClauses.listIterator(); i.hasNext();) {
			sb.append(i.nextIndex() == 0 ? " where " : " and ");
			sb.append(i.next());
		}
		sb.append(" group by e.patient_id ");
		for (ListIterator<String> i = havingClauses.listIterator(); i.hasNext();) {
			sb.append(i.nextIndex() == 0 ? " having " : " and ");
			sb.append(i.next());
		}
		log.debug("query: " + sb);
		
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sb.toString());
		if (encTypeIds != null) {
			query.setParameterList("encTypeIds", encTypeIds);
		}
		if (locationId != null) {
			query.setInteger("locationId", locationId);
		}
		if (formId != null) {
			query.setInteger("formId", formId);
		}
		if (fromDate != null) {
			query.setDate("fromDate", fromDate);
		}
		if (toDate != null) {
			query.setDate("toDate", toDate);
		}
		if (minCount != null) {
			query.setInteger("minCount", minCount);
		}
		if (maxCount != null) {
			query.setInteger("maxCount", maxCount);
		}
		
		return new Cohort(query.list());
	}
	
	public Cohort getPatientsHavingLocation(Integer locationId, PatientLocationMethod method) {
		StringBuffer sb = new StringBuffer();
		boolean argumentAsString = false;
		if (method == PatientLocationMethod.ANY_ENCOUNTER) {
			sb.append(" select e.patient_id from ");
			sb.append(" encounter e ");
			sb.append(" inner join patient p on e.patient_id = p.patient_id and p.voided = false ");
			sb.append(" where e.location_id = :location_id ");
			sb.append(" group by e.patient_id ");
		} else if (method == PatientLocationMethod.EARLIEST_ENCOUNTER) {
			sb.append(" select e.patient_id ");
			sb.append(" from encounter e ");
			sb.append("   inner join patient p on e.patient_id = p.patient_id and p.voided = false ");
			sb.append("   inner join (");
			sb.append("       select patient_id, min(encounter_datetime) as earliest ");
			sb.append("       from encounter ");
			sb.append("       group by patient_id) subq ");
			sb.append("     on e.patient_id = subq.patient_id and e.encounter_datetime = subq.earliest ");
			sb.append(" where e.location_id = :location_id ");
			sb.append(" group by e.patient_id ");
		} else if (method == PatientLocationMethod.LATEST_ENCOUNTER) {
			sb.append(" select e.patient_id ");
			sb.append(" from encounter e ");
			sb.append("   inner join patient p on e.patient_id = p.patient_id and p.voided = false ");
			sb.append("   inner join (");
			sb.append("       select patient_id, max(encounter_datetime) as earliest ");
			sb.append("       from encounter ");
			sb.append("       group by patient_id) subq ");
			sb.append("     on e.patient_id = subq.patient_id and e.encounter_datetime = subq.earliest ");
			sb.append(" where e.location_id = :location_id ");
			sb.append(" group by e.patient_id ");
		} else {
			sb.append(" select patient_id from patient p, person_attribute attr, person_attribute_type type ");
			sb.append(" where type.name = 'Health Center' ");
			sb.append(" and type.person_attribute_type_id = attr.person_attribute_type_id ");
			sb.append(" and attr.value = :location_id ");
			sb.append(" and attr.person_id = p.patient_id ");
			sb.append(" and attr.voided = false ");
			sb.append(" and p.voided = false ");
			argumentAsString = true;
		}
		log.debug("query: " + sb);
		
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sb.toString());
		
		if (argumentAsString) {
			query.setString("location_id", locationId.toString());
		} else {
			query.setInteger("location_id", locationId);
		}
		
		return new Cohort(query.list());
	}
	
	public Cohort getPatientsHavingObs(Integer conceptId, TimeModifier timeModifier,
	                                   Modifier modifier, Object value, Date fromDate, Date toDate) {
		if (conceptId == null && value == null) {
			throw new IllegalArgumentException("Can't have conceptId == null and value == null");
		}
		if (conceptId == null && (timeModifier != TimeModifier.ANY && timeModifier != TimeModifier.NO)) {
			throw new IllegalArgumentException("If conceptId == null, timeModifier must be ANY or NO");
		}
		if (conceptId == null && modifier != Modifier.EQUAL) {
			throw new IllegalArgumentException("If conceptId == null, modifier must be EQUAL");
		}
		Concept concept = null;
		if (conceptId != null) {
			concept = Context.getConceptService().getConcept(conceptId);
		}
		Number numericValue = null;
		String stringValue = null;
		Concept codedValue = null;
		Date dateValue = null;
		String valueSql = null;
		if (value != null) {
			if (concept == null) {
				if (value instanceof Concept) {
					codedValue = (Concept) value;
				} else {
					codedValue = Context.getConceptService().getConceptByName(value.toString());
				}
				valueSql = "o.value_coded";
			} else if (concept.getDatatype().isNumeric()) {
				if (value instanceof Number) {
					numericValue = (Number) value;
				} else {
					numericValue = new Double(value.toString());
				}
				valueSql = "o.value_numeric";
			} else if (concept.getDatatype().isText()) {
				stringValue = value.toString();
				valueSql = "o.value_text";
				if (modifier == null) {
					modifier = Modifier.EQUAL;
				}
			} else if (concept.getDatatype().isCoded()) {
				if (value instanceof Concept) {
					codedValue = (Concept) value;
				} else {
					codedValue = Context.getConceptService().getConceptByName(value.toString());
				}
				valueSql = "o.value_coded";
			} else if (concept.getDatatype().isDate()) {
				if (value instanceof Date) {
					dateValue = (Date) value;
				} else {
					try {
						dateValue = Context.getDateFormat().parse(value.toString());
					}
					catch (ParseException ex) {
						throw new IllegalArgumentException(
						        "Cannot interpret " + dateValue + " as a date in the format " + Context.getDateFormat());
					}
				}
				valueSql = "o.value_datetime";
			} else if (concept.getDatatype().isBoolean()) {
				if (value instanceof Concept) {
					codedValue = (Concept) value;
				} else {
					boolean asBoolean = false;
					if (value instanceof Boolean) {
						asBoolean = ((Boolean) value).booleanValue();
					} else {
						asBoolean = Boolean.valueOf(value.toString());
					}
					codedValue = asBoolean ? Context.getConceptService().getTrueConcept()
					        : Context.getConceptService().getFalseConcept();
				}
				valueSql = "o.value_coded";
			}
		}
		
		StringBuilder sb = new StringBuilder();
		boolean useValue = value != null;
		boolean doSqlAggregation = timeModifier == TimeModifier.MIN || timeModifier == TimeModifier.MAX
		        || timeModifier == TimeModifier.AVG;
		boolean doInvert = false;
		
		String dateSql = "";
		String dateSqlForSubquery = "";
		if (fromDate != null) {
			dateSql += " and o.obs_datetime >= :fromDate ";
			dateSqlForSubquery += " and obs_datetime >= :fromDate ";
		}
		if (toDate != null) {
			dateSql += " and o.obs_datetime <= :toDate ";
			dateSqlForSubquery += " and obs_datetime <= :toDate ";
		}
		
		if (timeModifier == TimeModifier.ANY || timeModifier == TimeModifier.NO) {
			if (timeModifier == TimeModifier.NO) {
				doInvert = true;
			}
			sb.append(
			    "select o.person_id from obs o " + "inner join patient p on o.person_id = p.patient_id and p.voided = false "
			            + "where o.voided = false ");
			if (conceptId != null) {
				sb.append("and concept_id = :concept_id ");
			}
			sb.append(dateSql);
			
		} else if (timeModifier == TimeModifier.FIRST || timeModifier == TimeModifier.LAST) {
			boolean isFirst = timeModifier == TimeModifier.FIRST;
			sb.append("select o.person_id " + "from obs o inner join (" + "    select person_id, "
			        + (isFirst ? "min" : "max") + "(obs_datetime) as obs_datetime" + "    from obs"
			        + "    where voided = false and concept_id = :concept_id " + dateSqlForSubquery
			        + "    group by person_id"
			        + ") subq on o.person_id = subq.person_id and o.obs_datetime = subq.obs_datetime "
			        + " inner join patient p on o.person_id = p.patient_id and p.voided = false "
			        + "where o.voided = false and o.concept_id = :concept_id ");
			
		} else if (doSqlAggregation) {
			String sqlAggregator = timeModifier.toString();
			valueSql = sqlAggregator + "(" + valueSql + ")";
			sb.append("select o.person_id " + "from obs o "
			        + "inner join patient p on o.person_id = p.patient_id and p.voided = false "
			        + "where o.voided = false and concept_id = :concept_id " + dateSql + "group by o.person_id ");
			
		} else {
			throw new IllegalArgumentException("TimeModifier '" + timeModifier + "' not recognized");
		}
		
		if (useValue) {
			sb.append(doSqlAggregation ? " having " : " and ");
			sb.append(valueSql + " ");
			sb.append(modifier.getSqlRepresentation() + " :value");
		}
		if (!doSqlAggregation) {
			sb.append(" group by o.person_id ");
		}
		
		log.debug("query: " + sb);
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sb.toString());
		query.setCacheMode(CacheMode.IGNORE);
		
		if (conceptId != null) {
			query.setInteger("concept_id", conceptId);
		}
		if (useValue) {
			if (numericValue != null) {
				query.setDouble("value", numericValue.doubleValue());
			} else if (codedValue != null) {
				query.setInteger("value", codedValue.getConceptId());
			} else if (stringValue != null) {
				query.setString("value", stringValue);
			} else if (dateValue != null) {
				query.setDate("value", dateValue);
			} else {
				throw new IllegalArgumentException(
				        "useValue is true, but numeric, coded, string, boolean, and date values are all null");
			}
		}
		if (fromDate != null) {
			query.setDate("fromDate", fromDate);
		}
		if (toDate != null) {
			query.setDate("toDate", toDate);
		}
		
		Cohort ret;
		if (doInvert) {
			ret = getAllPatients();
			ret.getMemberIds().removeAll(query.list());
		} else {
			ret = new Cohort(query.list());
		}
		
		return ret;
	}
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly) throws DAOException {
		return getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, null, null, minAge, maxAge, aliveOnly, deadOnly, null);
	}
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly) throws DAOException {
		return getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, minDeathdate, maxDeathdate, minAge, maxAge, aliveOnly, deadOnly, null);
	}
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Integer minAge,
	                                           Integer maxAge, Boolean aliveOnly, Boolean deadOnly, Date effectiveDate) {
		return getPatientsByCharacteristics(gender, minBirthdate, maxBirthdate, null, null, minAge, maxAge, aliveOnly, deadOnly, effectiveDate);
	}
	
	public Cohort getPatientsByCharacteristics(String gender, Date minBirthdate, Date maxBirthdate, Date minDeathdate, Date maxDeathdate, Integer minAge,
	                        Integer maxAge, Boolean aliveOnly, Boolean deadOnly, Date effectiveDate) throws DAOException {
		
		if (effectiveDate == null) {
			effectiveDate = new Date();
		}
		
		StringBuffer queryString = new StringBuffer("select patientId from Patient patient");
		List<String> clauses = new ArrayList<String>();
		
		clauses.add("patient.voided = false");
		
		if (gender != null) {
			gender = gender.toUpperCase();
			clauses.add("patient.gender = :gender");
		}
		if (minBirthdate != null) {
			clauses.add("patient.birthdate >= :minBirthdate");
		}
		if (maxBirthdate != null) {
			clauses.add("patient.birthdate <= :maxBirthdate");
		}
		if (minDeathdate != null) {
			clauses.add("patient.deathDate >= :minDeathdate");
		}
		if (maxDeathdate != null) {
			clauses.add("patient.deathDate <= :maxDeathdate");
		}
		if (aliveOnly != null && aliveOnly) {
			clauses.add("patient.dead = false");
		}
		if (deadOnly != null && deadOnly) {
			clauses.add("patient.dead = true");
		}
		
		Date maxBirthFromAge = null;
		if (minAge != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(effectiveDate);
			cal.add(Calendar.YEAR, -minAge);
			maxBirthFromAge = cal.getTime();
			clauses.add("patient.birthdate <= :maxBirthFromAge");
		}
		Date minBirthFromAge = null;
		if (maxAge != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(effectiveDate);
			cal.add(Calendar.YEAR, -(maxAge + 1));
			minBirthFromAge = cal.getTime();
			clauses.add("patient.birthdate > :minBirthFromAge");
		}
		
		clauses.add("(patient.birthdate is null or patient.birthdate <= :effectiveDate)");
		
		boolean first = true;
		for (String clause : clauses) {
			if (first) {
				queryString.append(" where ").append(clause);
				first = false;
			} else {
				queryString.append(" and ").append(clause);
			}
		}
		Query query = sessionFactory.getCurrentSession().createQuery(queryString.toString());
		query.setCacheMode(CacheMode.IGNORE);
		if (gender != null) {
			query.setString("gender", gender);
		}
		if (minBirthdate != null) {
			query.setDate("minBirthdate", minBirthdate);
		}
		if (maxBirthdate != null) {
			query.setDate("maxBirthdate", maxBirthdate);
		}
		if (minDeathdate != null) {
			query.setDate("minDeathdate", minDeathdate);
		}
		if (maxDeathdate != null) {
			query.setDate("maxDeathdate", maxDeathdate);
		}
		if (minAge != null) {
			query.setDate("maxBirthFromAge", maxBirthFromAge);
		}
		if (maxAge != null) {
			query.setDate("minBirthFromAge", minBirthFromAge);
		}
		query.setDate("effectiveDate", effectiveDate);
		
		return new Cohort(query.list());
	}
	
	public Cohort getPatientsHavingPersonAttribute(PersonAttributeType attribute, String value) {
		StringBuilder sb = new StringBuilder();
		sb.append(" select pat.patient_id ");
		sb.append(" from person p ");
		sb.append(" inner join patient pat on pat.patient_id = p.person_id and pat.voided = false ");
		sb.append(" inner join person_attribute a on p.person_id = a.person_id and a.voided = false ");
		sb.append(" where p.voided = false ");
		if (attribute != null) {
			sb.append(" and a.person_attribute_type_id = :typeId ");
		}
		if (value != null) {
			sb.append(" and a.value = :value ");
		}
		sb.append(" group by pat.patient_id ");
		log.debug("query: " + sb);
		
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sb.toString());
		if (attribute != null) {
			query.setInteger("typeId", attribute.getPersonAttributeTypeId());
		}
		if (value != null) {
			query.setString("value", value);
		}
		
		return new Cohort(query.list());
	}
	
	/**
	 * a given program. If fromDate != null, then only those patients who were in the program at any
	 * time after that date if toDate != null, then only those patients who were in the program at
	 * any time before that date
	 */
	public Cohort getPatientsInProgram(Integer programId, Date fromDate, Date toDate) {
		String sql = "select pp.patient_id from patient_program pp ";
		sql += " inner join patient p on pp.patient_id = p.patient_id and p.voided = false ";
		sql += " where pp.voided = false and pp.program_id = :programId ";
		if (fromDate != null) {
			sql += " and (date_completed is null or date_completed >= :fromDate) ";
		}
		if (toDate != null) {
			sql += " and (date_enrolled is null or date_enrolled <= :toDate) ";
		}
		log.debug("sql: " + sql);
		
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sql);
		query.setCacheMode(CacheMode.IGNORE);
		
		query.setInteger("programId", programId);
		if (fromDate != null) {
			query.setDate("fromDate", fromDate);
		}
		if (toDate != null) {
			query.setDate("toDate", toDate);
		}
		
		return new Cohort(query.list());
	}
	
	/**
	 * given program, workflow, and state, within a given date range
	 * 
	 * @param program The program the patient must have been in
	 * @param stateList List of states the patient must have been in (implies a workflow) (can be
	 *            null)
	 * @param fromDate If not null, then only patients in the given program/workflow/state on or
	 *            after this date
	 * @param toDate If not null, then only patients in the given program/workflow/state on or
	 *            before this date
	 * @return Cohort of Patients matching criteria
	 */
	public Cohort getPatientsByProgramAndState(Program program, List<ProgramWorkflowState> stateList, Date fromDate,
	        Date toDate) {
		Integer programId = program == null ? null : program.getProgramId();
		List<Integer> stateIds = null;
		if (stateList != null && stateList.size() > 0) {
			stateIds = new ArrayList<Integer>();
			for (ProgramWorkflowState state : stateList) {
				stateIds.add(state.getProgramWorkflowStateId());
			}
		}
		
		List<String> clauses = new ArrayList<String>();
		clauses.add("pp.voided = false");
		if (programId != null) {
			clauses.add("pp.program_id = :programId");
		}
		if (stateIds != null) {
			clauses.add("ps.state in (:stateIds)");
			clauses.add("ps.voided = false");
		}
		if (fromDate != null) {
			clauses.add("(pp.date_completed is null or pp.date_completed >= :fromDate)");
			if (stateIds != null) {
				clauses.add("(ps.end_date is null or ps.end_date >= :fromDate)");
			}
		}
		if (toDate != null) {
			clauses.add("(pp.date_enrolled is null or pp.date_enrolled <= :toDate)");
			if (stateIds != null) {
				clauses.add("(ps.start_date is null or ps.start_date <= :toDate)");
			}
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("select pp.patient_id ");
		sql.append("from patient_program pp ");
		sql.append("inner join patient p on pp.patient_id = p.patient_id and p.voided = false ");
		if (stateIds != null) {
			sql.append("inner join patient_state ps on pp.patient_program_id = ps.patient_program_id ");
		}
		for (ListIterator<String> i = clauses.listIterator(); i.hasNext();) {
			sql.append(i.nextIndex() == 0 ? " where " : " and ");
			sql.append(i.next());
		}
		sql.append(" group by pp.patient_id");
		log.debug("query: " + sql);
		
		Query query = sessionFactory.getCurrentSession().createSQLQuery(sql.toString());
		if (programId != null) {
			query.setInteger("programId", programId);
		}
		if (stateIds != null) {
			query.setParameterList("stateIds", stateIds);
		}
		if (fromDate != null) {
			query.setDate("fromDate", fromDate);
		}
		if (toDate != null) {
			query.setDate("toDate", toDate);
		}
		
		return new Cohort(query.list());
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, List<DrugOrder>> getCurrentDrugOrders(Cohort patients, List<Concept> drugConcepts)
	        throws DAOException {
		Map<Integer, List<DrugOrder>> ret = new HashMap<Integer, List<DrugOrder>>();
		
		Date now = new Date();
		
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(DrugOrder.class);
		criteria.setFetchMode("patient", FetchMode.JOIN);
		criteria.setCacheMode(CacheMode.IGNORE);
		
		// this "where clause" is only necessary if patients were passed in
		if (patients != null) {
			criteria.add(Restrictions.in("patient.personId", patients.getMemberIds()));
		}
		
		//criteria.add(Restrictions.in("encounter.patient.personId", ids));
		//criteria.createCriteria("encounter").add(Restrictions.in("patient.personId", ids));
		if (drugConcepts != null) {
			criteria.add(Restrictions.in("concept", drugConcepts));
		}
		criteria.add(Restrictions.eq("voided", false));
		criteria.add(Restrictions.le("dateActivated", now));
		criteria.add(Restrictions.and(Restrictions.or(Restrictions.isNull("autoExpireDate"), Restrictions.gt(
		    "autoExpireDate", now)), Restrictions
		        .or(Restrictions.isNull("dateStopped"), Restrictions.gt("dateStopped", now))));
		criteria.addOrder(org.hibernate.criterion.Order.asc("dateActivated"));
		log.debug("criteria: " + criteria);
		List<DrugOrder> temp = criteria.list();
		for (DrugOrder regimen : temp) {
			Integer ptId = regimen.getPatient().getPatientId();
			List<DrugOrder> list = ret.get(ptId);
			if (list == null) {
				list = new ArrayList<DrugOrder>();
				ret.put(ptId, list);
			}
			list.add(regimen);
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, List<Relationship>> getRelationships(Cohort patients, RelationshipType relType) {
		Map<Integer, List<Relationship>> ret = new HashMap<Integer, List<Relationship>>();
		
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Relationship.class);
		criteria.setCacheMode(CacheMode.IGNORE);
		if (relType != null) {
			criteria.add(Restrictions.eq("relationshipType", relType));
		}
		
		// this "where clause" is only useful if patients were passed in
		if (patients != null) {
			criteria.createCriteria("personB").add(Restrictions.in("personId", patients.getMemberIds()));
		}
		
		criteria.add(Restrictions.eq("voided", false));
		log.debug("criteria: " + criteria);
		List<Relationship> temp = criteria.list();
		for (Relationship rel : temp) {
			Integer ptId = rel.getPersonB().getPersonId();
			List<Relationship> rels = ret.get(ptId);
			if (rels == null) {
				rels = new ArrayList<Relationship>();
				ret.put(ptId, rels);
			}
			rels.add(rel);
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, List<DrugOrder>> getDrugOrders(Cohort patients, List<Concept> drugConcepts) throws DAOException {
		Map<Integer, List<DrugOrder>> ret = new HashMap<Integer, List<DrugOrder>>();
		if (patients != null && patients.size() == 0) {
			return ret;
		}
		
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(DrugOrder.class);
		criteria.setFetchMode("patient", FetchMode.JOIN);
		criteria.setCacheMode(CacheMode.IGNORE);
		
		// only include this where clause if patients were passed in
		if (patients != null) {
			criteria.add(Restrictions.in("patient.personId", patients.getMemberIds()));
		}
		
		if (drugConcepts != null) {
			criteria.add(Restrictions.in("concept", drugConcepts));
		}
		criteria.add(Restrictions.eq("voided", false));
		criteria.addOrder(org.hibernate.criterion.Order.asc("dateActivated"));
		log.debug("criteria: " + criteria);
		List<DrugOrder> temp = criteria.list();
		for (DrugOrder regimen : temp) {
			Integer ptId = regimen.getPatient().getPatientId();
			List<DrugOrder> list = ret.get(ptId);
			if (list == null) {
				list = new ArrayList<DrugOrder>();
				ret.put(ptId, list);
			}
			list.add(regimen);
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, Encounter> getFirstEncountersByType(Cohort patients, List<EncounterType> types) {
		Map<Integer, Encounter> ret = new HashMap<Integer, Encounter>();
		
		// default query
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Encounter.class);
		criteria.setCacheMode(CacheMode.IGNORE);
		
		// this "where clause" is only needed if patients were specified
		if (patients != null) {
			criteria.add(Restrictions.in("patient.personId", patients.getMemberIds()));
		}
		
		criteria.add(Restrictions.eq("voided", false));
		
		if (types != null && types.size() > 0) {
			criteria.add(Restrictions.in("encounterType", types));
		}
		
		criteria.addOrder(org.hibernate.criterion.Order.desc("patient.personId"));
		criteria.addOrder(org.hibernate.criterion.Order.asc("encounterDatetime"));
		
		List<Encounter> encounters = criteria.list();
		
		// set up the return map
		for (Encounter enc : encounters) {
			Integer ptId = enc.getPatient().getPatientId();
			if (!ret.containsKey(ptId)) {
				ret.put(ptId, enc);
			}
		}
		
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, Object> getEncounterAttrsByType(Cohort patients, List<EncounterType> encTypes, String attr,
	        Boolean earliestFirst) {
		Map<Integer, Object> ret = new HashMap<Integer, Object>();
		
		// default query
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Encounter.class);
		criteria.setCacheMode(CacheMode.IGNORE);
		
		// this "where clause" is only necessary if patients were specified
		if (patients != null) {
			criteria.add(Restrictions.in("patient.personId", patients.getMemberIds()));
		}
		
		criteria.add(Restrictions.eq("voided", false));
		
		if (encTypes != null && encTypes.size() > 0) {
			criteria.add(Restrictions.in("encounterType", encTypes));
		}
		
		criteria.setProjection(Projections.projectionList().add(Projections.property("patient.personId")).add(
		    Projections.property(attr)));
		
		criteria.addOrder(org.hibernate.criterion.Order.desc("patient.personId"));
		
		if (earliestFirst) {
			criteria.addOrder(org.hibernate.criterion.Order.asc("encounterDatetime"));
		} else {
			criteria.addOrder(org.hibernate.criterion.Order.desc("encounterDatetime"));
		}
		
		List<Object[]> attrs = criteria.list();
		
		// set up the return map
		for (Object[] row : attrs) {
			Integer ptId = (Integer) row[0];
			if (!ret.containsKey(ptId)) {
				ret.put(ptId, row[1]);
			}
		}
		
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, Encounter> getEncountersByType(Cohort patients, List<EncounterType> encTypes) {
		Map<Integer, Encounter> ret = new HashMap<Integer, Encounter>();
		
		// default query
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(Encounter.class);
		criteria.setCacheMode(CacheMode.IGNORE);
		
		// this "where clause" is only necessary if patients were passed in
		if (patients != null && patients.size() > 0) {
			criteria.add(Restrictions.in("patient.personId", patients.getMemberIds()));
		}
		
		criteria.add(Restrictions.eq("voided", false));
		
		if (encTypes != null && encTypes.size() > 0) {
			criteria.add(Restrictions.in("encounterType", encTypes));
		}
		
		criteria.addOrder(org.hibernate.criterion.Order.desc("patient.personId"));
		criteria.addOrder(org.hibernate.criterion.Order.desc("encounterDatetime"));
		
		List<Encounter> encounters = criteria.list();
		
		// set up the return map
		for (Encounter enc : encounters) {
			Integer ptId = enc.getPatient().getPatientId();
			if (!ret.containsKey(ptId)) {
				ret.put(ptId, enc);
			}
		}
		
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, List<List<Object>>> getObservationsValues(Cohort patients, Concept c, List<String> attributes,
	        Integer limit, boolean showMostRecentFirst) {
		Map<Integer, List<List<Object>>> ret = new HashMap<Integer, List<List<Object>>>();
		
		List<String> aliases = new Vector<String>();
		Boolean conditional = false;
		
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria("org.openmrs.Obs", "obs");
		criteria.setCacheMode(CacheMode.IGNORE);
		
		List<String> columns = new Vector<String>();
		
		for (String attribute : attributes) {
			List<String> classNames = new Vector<String>();
			if (attribute == null) {
				columns = findObsValueColumnName(c);
				if (columns.size() > 1) {
					conditional = true;
				}
				continue;
				//log.debug("c: " + c.getConceptId() + " attribute: " + attribute);
			} else if ("valueDate".equals(attribute)) {
				// pass -- same column name
			} else if ("valueTime".equals(attribute)) {
				// pass -- same column name
			} else if ("valueDatetime".equals(attribute)) {
				// pass -- same column name
			} else if ("obsDatetime".equals(attribute)) {
				// pass -- same column name
			} else if ("location".equals(attribute)) {
				// pass -- same column name
				classNames.add("obs.location");
				attribute = "location.name";
			} else if ("comment".equals(attribute)) {
				// pass -- same column name
			} else if ("encouterType".equals(attribute)) {
				classNames.add("obs.encounter");
				classNames.add("encounter.encounterType");
				attribute = "encounterType.name";
			} else if ("provider".equals(attribute)) {
				classNames.add("obs.encounter");
				classNames.add("encounter.encounterProviders");
				attribute = "encounterProviders.provider";
			} else {
				throw new DAOException("Attribute: " + attribute + " is not recognized. Please add reference in "
				        + this.getClass());
			}
			// if aliasing is necessary
			for (String className : classNames) {
				// if we haven't aliased this already
				if (!aliases.contains(className)) {
					criteria.createAlias(className, className.split("\\.")[1]);
					aliases.add(className);
				}
			}
			
			columns.add(attribute);
		}
		
		String aliasName = "obs";
		
		// set up the query
		ProjectionList projections = Projections.projectionList();
		projections.add(Projections.property("obs.personId"));
		for (String col : columns) {
			if (col.contains(".")) {
				projections.add(Projections.property(col));
			} else {
				projections.add(Projections.property(aliasName + "." + col));
			}
		}
		criteria.setProjection(projections);
		
		// only restrict on patient ids if some were passed in
		if (patients != null) {
			criteria.add(Restrictions.in("obs.personId", patients.getMemberIds()));
		}
		
		criteria.add(Restrictions.eq("obs.concept", c));
		criteria.add(Restrictions.eq("obs.voided", false));
		
		if (showMostRecentFirst) {
			criteria.addOrder(org.hibernate.criterion.Order.desc("obs.obsDatetime"));
		} else {
			criteria.addOrder(org.hibernate.criterion.Order.asc("obs.obsDatetime"));
		}
		
		long start = System.currentTimeMillis();
		List<Object[]> rows = criteria.list();
		log.debug("Took: " + (System.currentTimeMillis() - start) + " ms to run the patient/obs query");
		
		// set up the return map
		for (Object[] rowArray : rows) {
			//log.debug("row[0]: " + row[0] + " row[1]: " + row[1] + (row.length > 2 ? " row[2]: " + row[2] : ""));
			Integer ptId = (Integer) rowArray[0];
			
			List<List<Object>> oldArr = ret.get(ptId);
			
			// if we have already fetched all of the results the user wants 
			if (limit != null && limit > 0 && oldArr != null && oldArr.size() >= limit) {
				// the user provided a limit value and this patient already has more than
				// that number of values.
				// do nothing with this row
			} else {
				Boolean tmpConditional = conditional.booleanValue();
				
				// get all columns
				int index = 1;
				List<Object> row = new Vector<Object>();
				while (index < rowArray.length) {
					Object value = rowArray[index++];
					if (value instanceof Provider) {
						value = ((Provider)value).getName();
					}
					if (tmpConditional) {
						if (index == 2 && value != null) {
							// skip null first value if we must
							row.add(value);
						} else {
							row.add(rowArray[index]);
						}
						tmpConditional = false;
						// increment counter for next column.  (Skips over value_concept)
						index++;
					} else {
						row.add(value == null ? "" : value);
					}
				}
				
				// if we haven't seen a different row for this patient already:
				if (oldArr == null) {
					List<List<Object>> arr = new Vector<List<Object>>();
					arr.add(row);
					ret.put(ptId, arr);
				}
				// if we have seen a row for this patient already
				else {
					oldArr.add(row);
					ret.put(ptId, oldArr);
				}
			}
		}
		
		return ret;
	}
	
	public static List<String> findObsValueColumnName(Concept c) {
		String abbrev = c.getDatatype().getHl7Abbreviation();
		List<String> columns = new Vector<String>();
		
		if ("BIT".equals(abbrev)) {
			columns.add("valueCoded");
		} else if ("CWE".equals(abbrev)) {
			columns.add("valueDrug");
			columns.add("valueCoded");
		} else if ("NM".equals(abbrev) || "SN".equals(abbrev)) {
			columns.add("valueNumeric");
		} else if ("DT".equals(abbrev) || "TM".equals(abbrev) || "TS".equals(abbrev)) {
			columns.add("valueDatetime");
		} else if ("ST".equals(abbrev)) {
			columns.add("valueText");
		}
		
		return columns;
	}
	
	@SuppressWarnings("unchecked")
	public Map<Integer, Object> getPatientAttributes(Cohort patients, String className, String property, boolean returnAll)
	        throws DAOException {
		Map<Integer, Object> ret = new HashMap<Integer, Object>();
		
		className = "org.openmrs." + className;
		
		// default query
		Criteria criteria = null;
		
		// make 'patient.**' reference 'patient' like alias instead of object
		if ("org.openmrs.Patient".equals(className)) {
			criteria = sessionFactory.getCurrentSession().createCriteria("org.openmrs.Patient", "patient");
		} else if ("org.openmrs.Person".equals(className)) {
			criteria = sessionFactory.getCurrentSession().createCriteria("org.openmrs.Person", "person");
		} else {
			criteria = sessionFactory.getCurrentSession().createCriteria(className);
		}
		
		criteria.setCacheMode(CacheMode.IGNORE);
		
		// set up the query
		ProjectionList projectionList = Projections.projectionList();
		
		// if Person, PersonName, or PersonAddress
		if (className.contains("Person")) {
			projectionList.add(Projections.property("person.personId"));
			projectionList.add(Projections.property(property));
			
			if (patients != null) {
				criteria.add(Restrictions.in("person.personId", patients.getMemberIds()));
			}
			
			// do not include voided person rows
			if ("org.openmrs.Person".equals(className)) {
				// the voided column on the person table is mapped to the person object
				// through the getPersonVoided() to distinguish it from patient/user.voided
				criteria.add(Restrictions.eq("personVoided", false));
			} else {
				// this is here to support PersonName and PersonAddress
				criteria.add(Restrictions.eq("voided", false));
			}
		}
		// if one of the Patient tables
		else {
			projectionList.add(Projections.property("patient.personId"));
			projectionList.add(Projections.property(property));
			
			if (patients != null) {
				criteria.add(Restrictions.in("patient.personId", patients.getMemberIds()));
			}
			
			// do not include voided patients
			criteria.add(Restrictions.eq("voided", false));
		}
		criteria.setProjection(projectionList);
		
		// add 'preferred' sort order if necessary
		try {
			boolean hasPreferred = false;
			for (Field f : Class.forName(className).getDeclaredFields()) {
				if ("preferred".equals(f.getName())) {
					hasPreferred = true;
				}
			}
			
			if (hasPreferred) {
				criteria.addOrder(org.hibernate.criterion.Order.desc("preferred"));
			}
		}
		catch (ClassNotFoundException e) {
			log.warn("Class not found: " + className);
		}
		
		criteria.addOrder(org.hibernate.criterion.Order.desc("dateCreated"));
		List<Object[]> rows = criteria.list();
		
		// set up the return map
		if (returnAll) {
			for (Object[] row : rows) {
				Integer ptId = (Integer) row[0];
				Object columnValue = row[1];
				if (!ret.containsKey(ptId)) {
					Object[] arr = { columnValue };
					ret.put(ptId, arr);
				} else {
					Object[] oldArr = (Object[]) ret.get(ptId);
					Object[] newArr = new Object[oldArr.length + 1];
					System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
					newArr[oldArr.length] = columnValue;
					ret.put(ptId, newArr);
				}
			}
		} else {
			for (Object[] row : rows) {
				Integer ptId = (Integer) row[0];
				Object columnValue = row[1];
				if (!ret.containsKey(ptId)) {
					ret.put(ptId, columnValue);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * @param patients
	 * @param types List&lt;PatientIdentifierTypes&gt; of types to get
	 * @return Map of {@link PatientIdentifier}s
	 */
	@SuppressWarnings("unchecked")
	public Map<Integer, String> getPatientIdentifierByType(Cohort patients, List<PatientIdentifierType> types) {
		Map<Integer, String> patientIdentifiers = new HashMap<Integer, String>();
		
		// default query
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(PatientIdentifier.class);
		
		// only get the "identifier" and "patientId" columns
		ProjectionList projections = Projections.projectionList();
		projections.add(Projections.property("identifier"));
		projections.add(Projections.property("patient.personId"));
		criteria.setProjection(projections);
		
		criteria.setCacheMode(CacheMode.IGNORE);
		
		// Add patient restriction if necessary
		if (patients != null) {
			criteria.add(Restrictions.in("patient.personId", patients.getMemberIds()));
		}
		
		// all identifiers must be non-voided
		criteria.add(Restrictions.eq("voided", false));
		
		// Add identifier type filter
		if (types != null && types.size() > 0) {
			criteria.add(Restrictions.in("identifierType", types));
		}
		
		// Order by ID
		criteria.addOrder(org.hibernate.criterion.Order.desc("patient.personId"));
		
		List<Object[]> rows = criteria.list();
		
		// set up the return map
		for (Object[] row : rows) {
			String identifier = (String) row[0];
			Integer patientId = (Integer) row[1];
			if (!patientIdentifiers.containsKey(patientId)) {
				patientIdentifiers.put(patientId, identifier);
			}
		}
		
		return patientIdentifiers;
	}
	
	/**
	 * @see org.openmrs.api.db.PatientSetDAO#getPersonAttributes(org.openmrs.Cohort,
	 *      java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean)
	 */
	@SuppressWarnings("unchecked")
	public Map<Integer, Object> getPersonAttributes(Cohort patients, String attributeTypeName, String joinClass,
	        String joinProperty, String outputColumn, boolean returnAll) {
		Map<Integer, Object> ret = new HashMap<Integer, Object>();
		
		StringBuilder queryString = new StringBuilder();
		
		// set up the query
		queryString.append("select attr.person.personId, ");
		
		if (joinClass != null && joinProperty != null && outputColumn != null) {
			queryString.append("joinedClass.");
			queryString.append(outputColumn);
			queryString.append(" from PersonAttribute attr, PersonAttributeType t, ");
			queryString.append(joinClass);
			queryString.append(" joinedClass where t = attr.attributeType ");
			queryString.append("and attr.value = joinedClass.");
			queryString.append(joinProperty + " ");
		} else {
			queryString.append("attr.value from PersonAttribute attr, PersonAttributeType t where t = attr.attributeType ");
		}
		
		queryString.append("and t.name = :typeName ");
		queryString.append("order by attr.voided asc, attr.dateCreated desc");
		
		Query query = sessionFactory.getCurrentSession().createQuery(queryString.toString());
		query.setString("typeName", attributeTypeName);
		
		log.debug("query: " + queryString);
		
		List<Object[]> rows = query.list();
		
		// set up the return map
		for (Object[] row : rows) {
			Integer ptId = (Integer) row[0];
			if (patients == null || patients.contains(ptId)) {
				if (returnAll) {
					Object columnValue = row[1];
					if (!ret.containsKey(ptId)) {
						Object[] arr = { columnValue };
						ret.put(ptId, arr);
					} else {
						Object[] oldArr = (Object[]) ret.get(ptId);
						Object[] newArr = new Object[oldArr.length + 1];
						System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
						newArr[oldArr.length] = columnValue;
						ret.put(ptId, newArr);
					}
				} else {
					Object columnValue = row[1];
					if (!ret.containsKey(ptId)) {
						ret.put(ptId, columnValue);
					}
				}
			}
		}
		return ret;
	}
	
	/**
	 * This method assumes the patient is not simultaneously enrolled in the program more than once.
	 * if (includeVoided == true) then include voided programs if (includePast == true) then include
	 * program which are already complete In all cases this only returns the latest program
	 * enrollment for each patient.
	 */
	@SuppressWarnings("unchecked")
	public Map<Integer, PatientProgram> getPatientPrograms(Cohort ps, Program program, boolean includeVoided,
	        boolean includePast) throws DAOException {
		Map<Integer, PatientProgram> ret = new HashMap<Integer, PatientProgram>();
		
		Date now = new Date();
		
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(PatientProgram.class);
		criteria.setFetchMode("patient", FetchMode.JOIN);
		criteria.setCacheMode(CacheMode.IGNORE);
		
		// this "where clause" is only necessary if patients were passed in
		if (ps != null) {
			criteria.add(Restrictions.in("patient.personId", ps.getMemberIds()));
		}
		
		criteria.add(Restrictions.eq("program", program));
		if (!includeVoided) {
			criteria.add(Restrictions.eq("voided", false));
		}
		criteria.add(Restrictions.or(Restrictions.isNull("dateEnrolled"), Restrictions.le("dateEnrolled", now)));
		if (!includePast) {
			criteria.add(Restrictions.or(Restrictions.isNull("dateCompleted"), Restrictions.ge("dateCompleted", now)));
		}
		log.debug("criteria: " + criteria);
		List<PatientProgram> temp = criteria.list();
		for (PatientProgram prog : temp) {
			Integer ptId = prog.getPatient().getPatientId();
			ret.put(ptId, prog);
		}
		
		return ret;
	}
}