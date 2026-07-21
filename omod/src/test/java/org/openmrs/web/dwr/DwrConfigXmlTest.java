/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.web.dwr;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.openmrs.web.security.RequirePrivilege;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Regression guard for the two bugs fixed in RCM-124. {@code WebModuleUtil} only merges the
 * first {@code <dwr>} block per module into {@code dwr-modules.xml}, so a second block added by
 * mistake is silently dropped (this is exactly how {@code DWRCohortBuilderService}'s real
 * registration went missing for years). And {@code DwrAuthorizationFilter} (legacyui) fails open
 * on a DWR method with no {@link RequirePrivilege} rather than rejecting it, so a future method
 * added here without the annotation would ship silently unenforced - the only trace is a startup
 * WARN in legacyui's log, easy to miss. Mirrors {@code DwrAuthorizationFilter#scanDwrCreates} and
 * {@code #findMethodAnnotation} so this test fails whenever that filter's real allow-list would
 * actually be missing an entry.
 */
public class DwrConfigXmlTest {

	@Test
	public void configXml_shouldDeclareExactlyOneDwrBlock() throws Exception {
		NodeList dwrBlocks = loadConfigXml().getElementsByTagName("dwr");
		assertTrue("config.xml should declare exactly one <dwr> block - WebModuleUtil only merges "
		        + "the first one per module into dwr-modules.xml, so a second block is silently "
		        + "dropped rather than merged; found " + dwrBlocks.getLength(), dwrBlocks.getLength() == 1);
	}

	@Test
	public void configXml_everyIncludedDwrMethodShouldBeAnnotated() throws Exception {
		List<String> unannotated = new ArrayList<>();

		Element dwrBlock = (Element) loadConfigXml().getElementsByTagName("dwr").item(0);
		NodeList creates = dwrBlock.getElementsByTagName("create");
		for (int i = 0; i < creates.getLength(); i++) {
			Element create = (Element) creates.item(i);
			String script = create.getAttribute("javascript");
			String className = findClassParam(create);
			assertNotNull("DWR script '" + script + "' has no <param name=\"class\"/>", className);

			Class<?> dwrClass = Class.forName(className);

			NodeList includes = create.getElementsByTagName("include");
			for (int j = 0; j < includes.getLength(); j++) {
				String methodName = ((Element) includes.item(j)).getAttribute("method");
				if (findMethodAnnotation(dwrClass, methodName) == null) {
					unannotated.add(script + "." + methodName);
				}
			}
		}

		assertTrue("DWR methods exposed without @RequirePrivilege ship silently unenforced "
		        + "(DwrAuthorizationFilter fails open on them rather than rejecting): " + unannotated,
		    unannotated.isEmpty());
	}

	/**
	 * Mirrors {@code DwrAuthorizationFilter#findMethodAnnotation}: first declared method (walking
	 * up the class hierarchy) whose name matches, DWR resolves overloads by argument count at
	 * runtime but the filter treats all overloads of one name as sharing one privilege
	 * requirement.
	 */
	private RequirePrivilege findMethodAnnotation(Class<?> dwrClass, String methodName) {
		Class<?> cursor = dwrClass;
		while (cursor != null && cursor != Object.class) {
			for (Method method : cursor.getDeclaredMethods()) {
				if (method.getName().equals(methodName)) {
					RequirePrivilege annotation = method.getAnnotation(RequirePrivilege.class);
					if (annotation != null) {
						return annotation;
					}
				}
			}
			cursor = cursor.getSuperclass();
		}
		return null;
	}

	private String findClassParam(Element create) {
		NodeList params = create.getElementsByTagName("param");
		for (int i = 0; i < params.getLength(); i++) {
			Node node = params.item(i);
			if (!(node instanceof Element)) {
				continue;
			}
			Element param = (Element) node;
			if ("class".equals(param.getAttribute("name"))) {
				String value = param.getAttribute("value");
				if (value != null && !value.isEmpty()) {
					return value;
				}
			}
		}
		return null;
	}

	private Document loadConfigXml() throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
		dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.xml")) {
			assertNotNull("config.xml not found on test classpath", in);
			return dbf.newDocumentBuilder().parse(in);
		}
	}
}
