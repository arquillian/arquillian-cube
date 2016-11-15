package com.github.dockerjava.assertions;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.assertj.core.api.AbstractAssert;

/**
 * @author Eddú Meléndez
 */
public class ContainerStateAssert extends AbstractAssert<ContainerStateAssert,
		InspectContainerResponse.ContainerState> {

	public ContainerStateAssert(InspectContainerResponse.ContainerState actual) {
		super(actual, ContainerStateAssert.class);
	}

}
