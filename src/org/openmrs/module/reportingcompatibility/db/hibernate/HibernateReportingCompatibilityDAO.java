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
package org.openmrs.module.reportingcompatibility.db.hibernate;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.DAOException;
import org.openmrs.module.reportingcompatibility.db.ReportingCompatibilityDAO;
import org.openmrs.reporting.AbstractReportObject;
import org.openmrs.reporting.Report;
import org.openmrs.reporting.ReportObjectWrapper;

/**
 * Hibernate specific database methods for the ReportingCompatibilityDAO
 */
public class HibernateReportingCompatibilityDAO implements ReportingCompatibilityDAO {
	
	protected Log log = LogFactory.getLog(getClass());
	
	/**
	 * Hibernate session factory
	 */
	private SessionFactory sessionFactory;
	
	public HibernateReportingCompatibilityDAO() {}
	
	/**
	 * @see org.openmrs.api.AdministrationService#createReport(org.openmrs.reporting.Report)
	 */
	public void createReport(Report r) throws DAOException {
		r.setCreator(Context.getAuthenticatedUser());
		r.setDateCreated(new Date());
		sessionFactory.getCurrentSession().save(r);
	}
	
	/**
	 * @see org.openmrs.api.AdministrationService#updateReport(org.openmrs.reporting.Report)
	 */
	public void updateReport(Report r) throws DAOException {
		if (r.getReportId() == null)
			createReport(r);
		else {
			sessionFactory.getCurrentSession().saveOrUpdate(r);
		}
	}
	
	/**
	 * @see org.openmrs.api.AdministrationService#deleteReport(org.openmrs.reporting.Report)
	 */
	public void deleteReport(Report r) throws DAOException {
		sessionFactory.getCurrentSession().delete(r);
	}
	
	public void createReportObject(AbstractReportObject ro) throws DAOException {
		
		ReportObjectWrapper wrappedReportObject = new ReportObjectWrapper(ro);
		wrappedReportObject.setCreator(Context.getAuthenticatedUser());
		wrappedReportObject.setDateCreated(new Date());
		wrappedReportObject.setVoided(false);
		
		sessionFactory.getCurrentSession().save(wrappedReportObject);
	}
	
	public void updateReportObject(AbstractReportObject ro) throws DAOException {
		if (ro.getReportObjectId() == null)
			createReportObject(ro);
		else {
			sessionFactory.getCurrentSession().clear();
			ReportObjectWrapper wrappedReportObject = new ReportObjectWrapper(ro);
			wrappedReportObject.setChangedBy(Context.getAuthenticatedUser());
			wrappedReportObject.setDateChanged(new Date());
			sessionFactory.getCurrentSession().saveOrUpdate(wrappedReportObject);
		}
	}
	
	public void deleteReportObject(Integer reportObjectId) throws DAOException {
		ReportObjectWrapper wrappedReportObject = new ReportObjectWrapper();
		wrappedReportObject = (ReportObjectWrapper) sessionFactory.getCurrentSession().get(ReportObjectWrapper.class,
		    reportObjectId);
		
		sessionFactory.getCurrentSession().delete(wrappedReportObject);
	}
	
	/**
	 * Set session factory
	 * 
	 * @param sessionFactory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
}
