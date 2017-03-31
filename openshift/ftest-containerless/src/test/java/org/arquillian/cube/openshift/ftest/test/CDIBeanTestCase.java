package org.arquillian.cube.openshift.ftest.test;

import javax.inject.Inject;

import org.arquillian.cube.openshift.ftest.EnvPrinter;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

//@BelongsTo(IntegrationSuite.class)
@RunWith(Arquillian.class)
public class CDIBeanTestCase {

	@Inject
	private EnvPrinter env;

	@Test
	public void shouldRunInContainer() throws Exception {
		env.print();
	}

}
