package org.arquillian.cube.openshift.ftest;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EnvPrinter {

	public void print() {
		System.out.println(System.getenv());
	}
}
