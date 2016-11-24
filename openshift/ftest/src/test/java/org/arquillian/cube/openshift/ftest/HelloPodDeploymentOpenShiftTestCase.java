package org.arquillian.cube.openshift.ftest;

import java.net.URL;

import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class HelloPodDeploymentOpenShiftTestCase {

	@Deployment(testable = false)
	public static WebArchive deploy() {
		return ShrinkWrap.create(WebArchive.class)
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	@ArquillianResource
	private URL base;

	@Test
	public void shouldBeAbleToInjectURL() throws Exception {
		Assert.assertNotNull(base);
	}
}
