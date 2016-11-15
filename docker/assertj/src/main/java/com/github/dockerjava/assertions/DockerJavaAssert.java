package com.github.dockerjava.assertions;

import java.util.ArrayList;
import java.util.List;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Image;

/**
 * @author Eddú Meléndez
 */
public class DockerJavaAssert {

	private DockerClient client;

	public DockerJavaAssert(DockerClient client) {
		this.client = client;
	}

	public ImagesAssert hasImages(String... images) {
		List<Image> imageList = this.client.listImagesCmd().exec();
		ImagesAssert imagesAssert = new ImagesAssert(imageList);
		imagesAssert.containsImages(images);
		return imagesAssert;
	}

	public ContainerAssert container(String name) {
		InspectContainerResponse container = getContainerInformation(name);

		return new ContainerAssert(container);
	}

	public ContainersAssert containers(String... names) {
		List<InspectContainerResponse> containers = new ArrayList<InspectContainerResponse>();
		for (String containerName : names) {
			InspectContainerResponse container = getContainerInformation(containerName);
			if (container != null) {
				containers.add(container);
			}
		}

		return new ContainersAssert(containers);
	}

	private InspectContainerResponse getContainerInformation(String name) {
		return this.client.inspectContainerCmd(name).exec();
	}

}
