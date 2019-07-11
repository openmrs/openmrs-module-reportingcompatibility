
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.CareSetting.CareSettingType;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.api.context.Context;
import org.openmrs.cohort.Cohort;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.reportingcompatibility.service.ReportService;
import org.openmrs.module.reportingcompatibility.service.ReportService.GroupMethod;
import org.openmrs.report.EvaluationContext;
import org.openmrs.reporting.CachingPatientFilter;
import org.openmrs.util.OpenmrsUtil;

public class DrugOrderFilter extends CachingPatientFilter {
	 
	private static final long serialVersionUID = 1L;
	
	protected final Log log = LogFactory.getLog(getClass());
	
	private List<Drug> drugList;
	
	private GroupMethod anyOrAllOrNone;
	
	private CareSettingType inPatientOrOutPatient;
	
	private Integer untilDaysAgo;
	
	private Integer untilMonthsAgo;
	
	private List<Concept> drugSets;
	
	private List<Concept> drugConcepts;
	
	private List<Concept> conceptSets;
	
	private Date activeOnOrBefore;
	
	private Date activeOnOrAfter;
	
	private Integer activeWithinLastMonths;
	
	private Integer activeWithinLastDays;
	
	private boolean onlyCurrentlyActive;
	
	
	public DrugOrderFilter() {
		
		super.setType("Patient Filter");
		super.setSubType("Drug Order Filter");
	}
	
	@Override
	public String getCacheKey() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName()).append(".");
		sb.append(getAnyOrAllOrNone()).append(".");
		sb.append(OpenmrsUtil.fromDateHelper(null, activeWithinLastDays, activeWithinLastMonths, untilDaysAgo,
		    untilMonthsAgo, activeOnOrAfter, activeOnOrBefore)).append(".");
		sb.append(OpenmrsUtil.toDateHelper(null, activeWithinLastDays, activeWithinLastMonths, untilDaysAgo, untilMonthsAgo,
		    activeOnOrAfter, activeOnOrBefore)).append(".");
		if (getDrugListToUse() != null) {
			for (Drug d : getDrugListToUse()) {
				sb.append(d.getDrugId()).append(",");
			}
		}
		return sb.toString();
	}
	
	@Override
	public String getDescription() {
		MessageSourceService mss = Context.getMessageSourceService();
		Locale locale = Context.getLocale();
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Context.getLocale());
		StringBuilder ret = new StringBuilder();
		boolean onlyCurrentlyActive = getActiveWithinLastDays() != null && getActiveWithinLastDays() == 0
		        && (getActiveWithinLastMonths() == null || getActiveWithinLastMonths() == 0);
		if (onlyCurrentlyActive) {
			ret.append(mss.getMessage("reporting.patientCurrently")).append(" ");
		} else {
			ret.append(mss.getMessage("reporting.patients")).append(" ");
		}
		if (getDrugListToUse() == null || getDrugListToUse().isEmpty()) {
			if (getAnyOrAllOrNone() == GroupMethod.NONE) {
				ret.append(onlyCurrentlyActive ? mss.getMessage("reporting.takingNoDrugs")
				        : mss.getMessage("reporting.whoNeverTakeDrug"));
			}
			else if (getAnyOrAllOrNone() == GroupMethod.ANY) {
				ret.append(onlyCurrentlyActive ? mss.getMessage("reporting.takingAnyDrugs")
				        : mss.getMessage("reporting.everTakingAnyDrugs"));
			}
			else if (getAnyOrAllOrNone() == GroupMethod.ALL) {
				ret.append(onlyCurrentlyActive ? mss.getMessage("reporting.takingAllDrugs")
				        : mss.getMessage("reporting.whoTakingAllDrugs"));
			}
		} else {
			if (getDrugListToUse().size() == 1) {
				if (getAnyOrAllOrNone() == GroupMethod.NONE) {
					ret.append(mss.getMessage("reporting.notTaking")).append(" ");
				} else {
					ret.append(mss.getMessage("reporting.taking").toLowerCase()).append(" ");
				}
				ret.append(getDrugListToUse().get(0).getName());
			} else {
				ret.append(mss.getMessage("reporting.taking").toLowerCase()).append(" ").append(getAnyOrAllOrNone())
				        .append(mss.getMessage("reporting.of")).append(" [");
				for (Iterator<Drug> i = getDrugListToUse().iterator(); i.hasNext();) {
					ret.append(i.next().getName());
					if (i.hasNext()) {
						ret.append(" , ");
					}
				}
				ret.append("] ");
			}
			if (getInPatientOrOutPatient() == CareSettingType.OUTPATIENT) {
				ret.append(mss.getMessage("reporting.takingToHome"));
			} else if (getInPatientOrOutPatient() == CareSettingType.INPATIENT) {
				ret.append(mss.getMessage("reporting.takingToWard"));
			}
			//TODO CareSettingType.EMERGENCY
		}
		Integer active_within_last_days = getActiveWithinLastDays();
		Integer active_within_last_months = getActiveWithinLastMonths();
		
		if (!onlyCurrentlyActive) {
			if (active_within_last_days != null || active_within_last_months != null) {
				if (active_within_last_months != null)
					ret.append(" ").append(
					    mss.getMessage("reporting.WithinTheLastMonths", new Object[] { active_within_last_months }, locale));
				
				if (active_within_last_days != null)
					ret.append(" ").append(
					    mss.getMessage("reporting.WithinTheLastDays", new Object[] { active_within_last_days }, locale));
				
			}
		}
		if (getActiveOnOrAfter() != null) {
			ret.append(" ")
			        .append(mss.getMessage("reporting.activeOnOrAfter", new Object[] { df.format(getActiveOnOrAfter()) },
			            locale));
		}
		if (getActiveOnOrBefore() != null) {
			ret.append(" ")
			        .append(
			            mss.getMessage("reporting.onOrBefore", new Object[] { df.format(getActiveOnOrBefore()) }, locale));
		}
		return ret.toString();
	}
	
	@Override
	public Cohort filterImpl(EvaluationContext context) {
		List<Integer> drugIds = new ArrayList<Integer>();
		if (getDrugListToUse() != null) {
			for (Drug d : getDrugListToUse()) {
				drugIds.add(d.getDrugId());
			}
		}
		log.debug("filtering with these ids " + drugIds);
		Collection<Integer> patientIds = context == null ? null : context.getBaseCohort().getMemberIds();
		return Context.getService(ReportService.class).getPatientsHavingDrugOrder(patientIds, drugIds, getAnyOrAllOrNone(),
		    OpenmrsUtil.fromDateHelper(null, getActiveWithinLastDays(), getActiveWithinLastMonths(), getUntilDaysAgo(),
		        getUntilMonthsAgo(), getActiveOnOrAfter(), getActiveOnOrBefore()),
		    OpenmrsUtil.toDateHelper(null, getActiveWithinLastDays(), getActiveWithinLastMonths(), getUntilDaysAgo(),
		        getUntilMonthsAgo(), getActiveOnOrAfter(), getActiveOnOrBefore()));
	}
	
	@Override
	public boolean isReadyToRun() {
		return true;
	}
	
	public List<Drug> getDrugListToUse() {
		List<Drug> drugList = getDrugList();
		List<Concept> drugSets = getDrugSets();
		if (drugList == null && drugSets == null) {
			return null;
		}
		List<Drug> ret = new ArrayList<Drug>();
		if (drugList != null) {
			ret.addAll(drugList);
		}
		if (drugSets != null) {
			Set<Concept> generics = new HashSet<Concept>();
			for (Concept drugSet : drugSets) {
				setConceptSets(drugSet);
				List<Concept> conceptSets = getConceptSets();
				generics.addAll(conceptSets);
			}
			for (Concept generic : generics) {
				ret.addAll(Context.getConceptService().getDrugsByConcept(generic));
			}
		}
		
		return ret;
	}
	
	//getters and setters
	public void setConceptSets(Concept conceptSets) {
		
		this.conceptSets = Context.getConceptService().getConceptsByConceptSet(conceptSets);
	}
	
	public List<Concept> getConceptSets() {
		
		return conceptSets;
	}
	
	public void setDrugConcepts(List<Concept> drugConcepts) {
		
		this.drugConcepts = drugConcepts;
	}
	
	public List<Concept> getDrugConcepts() {
		
		return drugConcepts;
	}
	
	public void setDrugSets(List<Concept> drugSets) {
		
		this.drugSets = drugSets;
	}
	
	public List<Concept> getDrugSets() {
		
		return drugSets;
	}
	
	public void setActiveOnOrBefore(Date activeOnOrBefore) {
		
		this.activeOnOrBefore = activeOnOrBefore;
	}
	
	public Date getActiveOnOrBefore() {
		
		return activeOnOrBefore;
	}
	
	public GroupMethod getAnyOrAllOrNone() {
		return anyOrAllOrNone;
	}
	
	public void setAnyOrAllOrNone(GroupMethod anyOrAllOrNone) {
		this.anyOrAllOrNone = anyOrAllOrNone;
	}
	
	public CareSettingType getInPatientOrOutPatient() {
		
		return inPatientOrOutPatient;
	}
	
	public void setInPatientOrOutPatient(CareSettingType inPatientOrOutPatient) {
		
		this.inPatientOrOutPatient = inPatientOrOutPatient;
	}
	public List<Drug> getDrugList() {
		return drugList;
	}
	
	public void setDrugList(List<Drug> drugList) {
		this.drugList = drugList;
	}
	
	public void setActiveOnOrAfter(Date activeOnOrAfter) {
		
		this.activeOnOrAfter = activeOnOrAfter;
	}
	
	public Date getActiveOnOrAfter() {
		
		return activeOnOrAfter;
	}
	
	public void setActiveWithinLastMonths(Integer activeWithinLastMonths) {
		
		this.activeWithinLastMonths = activeWithinLastMonths;
	}
	
	public Integer getActiveWithinLastMonths() {
		
		return activeWithinLastMonths;
	}
	
	public void setActiveWithinLastDays(Integer activeWithinLastDays) {
		
		this.activeWithinLastDays = activeWithinLastDays;
	}
	
	public Integer getActiveWithinLastDays() {
		
		return activeWithinLastDays;
	}
	
	public void setOnlyCurrentlyActive(boolean onlyCurrentlyActive) {
		
		this.onlyCurrentlyActive = onlyCurrentlyActive;
	}
	
	public boolean getOnlyCurrentlyActive() {
		
		return onlyCurrentlyActive;
	}
	
	public Integer getUntilDaysAgo() {
		return untilDaysAgo;
	}
	
	public void setUntilDaysAgo(Integer untilDaysAgo) {
		this.untilDaysAgo = untilDaysAgo;
	}
	
	public Integer getUntilMonthsAgo() {
		return untilMonthsAgo;
	}
	
	public void setUntilMonthsAgo(Integer untilMonthsAgo) {
		this.untilMonthsAgo = untilMonthsAgo;
	}
	
}
