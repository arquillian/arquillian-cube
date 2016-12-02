package com.github.dockerjava.assertions;

import com.github.dockerjava.api.DockerClient;
import org.assertj.core.api.Assertions;

/**
 * @author Eddú Meléndez
 */
public class DockerJavaAssertions extends Assertions {

	public static DockerJavaAssert assertThat(DockerClient client) {
		return new DockerJavaAssert(client);
	}

}
