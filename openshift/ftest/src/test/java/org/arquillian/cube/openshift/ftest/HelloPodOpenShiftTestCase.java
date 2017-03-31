package org.arquillian.cube.openshift.ftest;

import org.arquillian.cube.CubeController;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@Ignore // hello-openshift container is auto started due to container mapping
@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class HelloPodOpenShiftTestCase {

	private static final String ID = "hello-openshift";

	@ArquillianResource
	private CubeController cc;

	@Test @InSequence(1)
	public void shouldbeAbleToCreateCube() {
		cc.create(ID);
	}

	@Test @InSequence(2)
	public void shouldbeAbleToStartCube() {
		cc.start(ID);
	}

	@Test @InSequence(3)
	public void shouldbeAbleToStopCube() {
		cc.stop(ID);
	}

	@Test @InSequence(4)
	public void shouldbeAbleToDestroyCube() {
		cc.destroy(ID);
	}
}
