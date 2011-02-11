package org.openmrs.module.reportingcompatibility;

import java.util.List;

public interface CohortBuilderLinkProvider {

	/**
	 * @return a List of Strings that specify links for the Links popup in cohort builder
 	 * An example link specification would be
	 *   "nealreport.form:reportingcompatibility.nealreport.ConsultReport.name:reportType=Consult Report"
	 * which is
	 *   url to submit to
	 *   :
	 *   message to localize for the label
	 *   :
	 *   parameter to submit to url, in addition to patientIds
	 */
	public List<String> getLinkSpecifications();

}