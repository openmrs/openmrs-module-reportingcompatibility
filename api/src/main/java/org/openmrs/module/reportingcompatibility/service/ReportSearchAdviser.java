/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

package org.openmrs.module.reportingcompatibility.service;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.Daemon;
import org.openmrs.cohort.Cohort;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

public class ReportSearchAdviser extends StaticMethodMatcherPointcutAdvisor implements Advisor {

    private static final Logger log = LoggerFactory.getLogger(ReportSearchAdviser.class);

    @Override
    public boolean matches(Method method, Class targetClass) {
        if (method.getName().equals("getPatientsHavingEncounters")){
            return true;
        }
    

    else if (method.getName().equals("getPatientsHavingDrugOrder")){
        return true;
    }

    else if (method.getName().equals("getAllPatients")){
        return true;
    }

    else if (method.getName().equals("getPatientsByProgramAndState")){
        return true;
    }

    else if (method.getName().equals("getPatientsInProgram")){
        return true;
    }

    else if (method.getName().equals("getPatientsHavingPersonAttribute")){
        return true;
    }

    else if (method.getName().equals("getPatientsByCharacteristics")){
        return true;
    }

    else if (method.getName().equals("getPatientsHavingObs")){
        return true;
    }

    else if (method.getName().equals("getPatientsHavingLocation")){
        return true;
    }

    return false;

}

    @Override
    public Advice getAdvice(){
        return new PReportSearchAdvise();
    }

    private class PReportSearchAdvise implements MethodInterceptor{
        public Object invoke(MethodInvocation invocation) throws Throwable{
            if (Context.getAuthenticatedUser() == null){
                return null;
            }
            Object object = invocation.proceed();
            if (Daemon.isDaemonUser(Context.getAuthenticatedUser()) || Context.getAuthenticatedUser().isSuperUser()){
                return object;
            }

            Cohort thisCohort = (Cohort) object;
            Set<Integer> patientsIds = thisCohort.getMemberIds();
            for (Iterator<Integer> iterator = patientsIds.iterator(); iterator.hasNext(); ){
                Integer patientIds = iterator.next();
                if (patientIds != null) {
                    Patient patient = Context.getPatientService().getPatient(patientIds);

                    if (patient == null){
                        iterator.remove();
                    }
                }
                else{
                    iterator.remove();
                }
            }
            return thisCohort;

        }
    }

}