package org.openmrs.module.reportingcompatibility.reporting;
 
import org.openmrs.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.reportingcompatibility.service.ReportingCompatibilityService;
import org.openmrs.report.EvaluationContext;
import org.openmrs.reporting.CachingPatientFilter;
import org.springframework.util.StringUtils; 
/**
 * This class implements a simple patient filter using a SQL query
 */
public class SqlPatientFilter extends CachingPatientFilter {
 
        /**
         * SQL query that will filter the patients
         */
        private String query;
 
        public SqlPatientFilter() {
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
        	// have tried to add the following code
        	// but it does not cause any difference
//        	// if the query is empty, return false
//        	if (query == null || query.isEmpty()){
//        		return false;
//        	}
//        	
//        	// verify that the query string is something like:
//        	// select *patient_id from
//        	String upStr = query.trim();
//        	int selectIdx = findIdx(upStr.toUpperCase(), "SELECT");
//        	int fromIdx = findIdx(upStr.toUpperCase(), "FROM");
//        	
//        	if (selectIdx < 0 || fromIdx < 0 || selectIdx - fromIdx >= 0){
//        		return false;
//        	}
//        	
//        	String selectClause = upStr.substring(selectIdx, fromIdx);
//        	if (selectClause.contains(",")){
//        		return false;
//        	}
//        	if (!selectClause.contains("patient_id")){
//        		return false;
//        	}
        	return true;
        }
        
        private int findIdx(String query, String pat){
        	int idx = query.indexOf(pat);
        	if (idx < 0 || idx > 0 && 
        			(Character.isLetterOrDigit(query.charAt(idx-1)) || 
        					query.charAt(idx-1) == '_' || query.charAt(idx-1) == '.'))
        		idx = -1;
        	return idx;
        }
        
        @Override
        public String getDescription() {
                StringBuilder sb = new StringBuilder();
                MessageSourceService msa = Context.getMessageSourceService();
                final String description = msa.getMessage("reporting.filter.desc") + ": ";
                sb.append(description);
                if (query != null)
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