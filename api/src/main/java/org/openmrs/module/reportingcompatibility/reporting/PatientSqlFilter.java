package org.openmrs.module.reportingcompatibility.reporting;

import org.openmrs.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.module.reportingcompatibility.service.ReportingCompatibilityService;
import org.openmrs.report.EvaluationContext;
import org.openmrs.reporting.CachingPatientFilter;

/**
 * This class implements a simple patient filter using a SQL query
 */
@Deprecated
public class PatientSqlFilter extends CachingPatientFilter {

	/**
	 * SQL query that will filter the patients
	 */
	private String query;

	public PatientSqlFilter() {
	}

	@Override
	public Cohort filterImpl(EvaluationContext context) {
		return Context.getService(ReportingCompatibilityService.class).getPatientsBySqlQuery(getQuery());
	}

	@Override
	public String getCacheKey() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getName()).append(".");
		sb.append(query.replaceAll("\n", ""));
		return sb.toString();
	}

	@Override
	public boolean isReadyToRun() {
		return true;
	}

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("Patients matching this query: ");
		sb.append(query.replaceAll("\n", " "));
		return sb.toString();
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

}
