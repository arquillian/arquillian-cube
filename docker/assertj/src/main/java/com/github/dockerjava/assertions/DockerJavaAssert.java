package com.github.dockerjava.assertions;

import com.github.dockerjava.CubeOutput;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.command.ExecStartResultCallback;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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

   public CubeOutputAssert withContainer(String name) {
         String[] cmd = {"sh", "-c", "for i in $(ps -axo pid --no-headers); do echo `ps -p $i -o comm=`; done"};

         return new CubeOutputAssert(getOutputOfCommand(name, cmd));
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

   private CubeOutput getOutputOfCommand(String containerId, String[] command) {
      String execResponseId = execCreate(containerId, command);

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      OutputStream errorStream = new ByteArrayOutputStream();

      try {
         this.client.execStartCmd(execResponseId).withDetach(false)
                 .exec(new ExecStartResultCallback(outputStream, errorStream)).awaitCompletion();
      } catch (InterruptedException e) {
         return new CubeOutput("", "");
      }

      return new CubeOutput(outputStream.toString().trim(), errorStream.toString().trim());
   }

   private String execCreate(String containerId, String... command) {
      return this.client.execCreateCmd(containerId)
              .withAttachStdout(true).withAttachStderr(true)
              .withAttachStdin(true).withTty(false)
              .withCmd(command).exec().getId();
   }
}
