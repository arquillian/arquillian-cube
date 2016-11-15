package com.github.dockerjava.assertions;

import java.util.List;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.assertj.core.api.ListAssert;

/**
 * @author Eddú Meléndez
 */
public class ContainersAssert extends ListAssert<InspectContainerResponse> {

	protected ContainersAssert(List<InspectContainerResponse> actual) {
		super(actual);
	}

	public ContainersAssert areRunning() {
		for (InspectContainerResponse container : this.actual) {
			ContainerStateAssert stateAssert = new ContainerStateAssert(container.getState());
			stateAssert.isNotNull();

			if (!container.getState().getRunning()) {
				failWithMessage("Container %s is not running", container.getName());
			}
		}

		return this;
	}

}
