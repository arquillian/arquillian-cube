package com.github.dockerjava.assertions

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.command.InspectContainerCmd
import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.command.ListImagesCmd
import com.github.dockerjava.api.model.ContainerConfig
import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.Image
import com.github.dockerjava.api.model.InternetProtocol
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import spock.lang.Specification

import static com.github.dockerjava.assertions.DockerJavaAssertions.assertThat

/**
 * @author Eddú Meléndez
 */
class DockerJavaAssertionsSpec extends Specification {

    private DockerClient client

    def setup() {
        this.client = Mock(DockerClient)
    }

    def "[foo, bar] images should exist"() {
        setup:
            ListImagesCmd cmd = Mock(ListImagesCmd)

            def images = [new Image(id: "foo"), new Image(id: "bar")]

        when:
            this.client.listImagesCmd() >> cmd
            this.client.listImagesCmd().exec() >> images

        then:
            assertThat(this.client).hasImages("foo", "bar")
    }

    def "[foo, bar] should be present but bar image doesn't exist"() {
        setup:
            ListImagesCmd cmd = Mock(ListImagesCmd)

            def images = [new Image(id: "foo")]

        when:
            this.client.listImagesCmd() >> cmd
            this.client.listImagesCmd().exec() >> images
            assertThat(this.client).hasImages("foo", "bar")

        then:
            AssertionError er = thrown()
            er.message == String.format("%nExpecting:%n <%s>%nto contain:%n <%s>", "[foo]", "[foo, bar]")
    }

    def "exposedPorts [8080/tcp, 80/tcp] should exist"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def exposedPorts = [new ExposedPort(8080), new ExposedPort(80, InternetProtocol.TCP)]
            def containerConfig = new ContainerConfig(exposedPorts: exposedPorts)
            def container = new InspectContainerResponse(config: containerConfig)

        when:
            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> container

        then:
            assertThat(this.client).container("java").hasExposedPorts("8080/tcp", "80/tcp")
    }

    def "portBindings [8080/tcp, 80/tcp] should exist"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def portBindings = new Ports(PortBinding.parse("8080"), PortBinding.parse("80/tcp"))
            def hostConfig = new HostConfig(portBindings: portBindings)
            def container = new InspectContainerResponse(hostConfig: hostConfig)

        when:
            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> container

        then:
            assertThat(this.client).container("java").hasBindPorts("8080/tcp", "80/tcp")
    }

    def "volumes [/var/jenkins_home, /var/jenkins_home2] should exist"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def volumes = ["/var/jenkins_home" : "", "/var/jenkins_home2" : ""]
            def containerConfig = new ContainerConfig(volumes: volumes)
            def container = new InspectContainerResponse(config: containerConfig)

        when:
            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> container

        then:
            assertThat(this.client).container("java").hasVolumes("/var/jenkins_home", "/var/jenkins_home2")
    }

    def "number of container's volumes should be 2 but it's one"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def volumes = ["/var/jenkins_home" : ""]
            def containerConfig = new ContainerConfig(volumes: volumes)
            def container = new InspectContainerResponse(config: containerConfig)

        when:
            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> container
            assertThat(this.client).container("java").hasVolumes ("/var/jenkins_home", "/var/jenkins_home2")

        then:
            AssertionError er = thrown()
            er.message == String.format("%nExpecting:%n <%s>%nto contain:%n <%s>", "[/var/jenkins_home]", "[/var/jenkins_home, /var/jenkins_home2]")
    }

    def "number of container's volumes should be 2"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def volumes = ["/var/jenkins_home" : "", "/var/jenkins_home2" : ""]
            def containerConfig = new ContainerConfig(volumes: volumes)
            def container = new InspectContainerResponse(config: containerConfig)

        when:
            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> container

        then:
            assertThat(this.client).container("java").hasVolumes(2)
    }

    def "number of container's volumes should be 3 but it is 2"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def volumes = ["/var/jenkins_home" : "", "/var/jenkins_home2" : ""]
            def containerConfig = new ContainerConfig(volumes: volumes)
            def container = new InspectContainerResponse(config: containerConfig)

        when:
            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> container
            assertThat(this.client).container("java").hasVolumes(3)

        then:
            AssertionError er = thrown()
            er.message == "Expected container's volumes to be 3 but was 2"
    }

    def "container's name is /my_java_container"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse(name: "/my_java_container")

        when:
            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> container

        then:
            assertThat(this.client).container("java").hasName("/my_java_container")
    }

    def "container's name should be /my_java_container but it isn't"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse(name: "/my_java_container_1")

        when:
            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> container
            assertThat(this.client).container("java").hasName("/my_java_container")

        then:
            AssertionError er = thrown()
            er.message == "Expected container's name to be /my_java_container but was /my_java_container_1"
    }

    def "container should be paused"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse()
            def containerState = new InspectContainerResponse.ContainerState(container, [paused: true])
            container.state = containerState

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> container

        then:
            assertThat(this.client).container("go").isPaused()
    }

    def "container should be paused but it isn't"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse()
            def containerState = new InspectContainerResponse.ContainerState(container, [paused: false])
            container.state = containerState

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> container
            assertThat(this.client).container("go").isPaused()

        then:
            AssertionError er = thrown()
            er.message == "Expected container's state paused to be true but was false"
    }

    def "container should be restarting"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse()
            def containerState = new InspectContainerResponse.ContainerState(container, [restarting: true])
            container.state = containerState

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> container

        then:
            assertThat(this.client).container("go").isRestarting()
    }

    def "container should be restarting but it isn't"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse()
            def containerState = new InspectContainerResponse.ContainerState(container, [restarting: false])
            container.state = containerState

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> container
            assertThat(this.client).container("go").isRestarting()

        then:
            AssertionError er = thrown()
            er.message == "Expected container's state restarting to be true but was false"
    }

    def "container should be running and status running"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse()
            def containerState = new InspectContainerResponse.ContainerState(container, [running: true])
            container.state = containerState

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> container

        then:
            assertThat(this.client).container("go").isRunning()
    }

    def "container's status should be running"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse()
            def containerState = new InspectContainerResponse.ContainerState(container, [status: "running"])
            container.state = containerState

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> container

        then:
            assertThat(this.client).container("go").hasStatus("running")
    }

    def "container's status should be restarting but it's running"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse()
            def containerState = new InspectContainerResponse.ContainerState(container, [status: "running"])
            container.state = containerState

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> container
            assertThat(this.client).container("go").hasStatus("restarting")

        then:
            AssertionError er = thrown()
            er.message == "Expected container's status to be restarting but was running"
    }

    def "container should be running but it isn't"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse()
            def containerState = new InspectContainerResponse.ContainerState(container, [running: false])
            container.state = containerState

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> container
            assertThat(this.client).container("go").isRunning()

        then:
            AssertionError er = thrown()
            er.message == "Expected container's state running to be true but was false"
    }

    def "two containers should be running"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def container = new InspectContainerResponse()
            def containerState = new InspectContainerResponse.ContainerState(container, [running: true])
            container.state = containerState

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> container

            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> container

        then:
            assertThat(this.client).containers("go", "java").areRunning()
    }

    def "two containers should be running but java container doesn't"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def dockerContainer = new InspectContainerResponse()
            def dockerContainerState = new InspectContainerResponse.ContainerState(dockerContainer, [running: true])
            dockerContainer.state = dockerContainerState

            def javaContainer = new InspectContainerResponse()
            def javaContainerState = new InspectContainerResponse.ContainerState(javaContainer, [running: false])
            javaContainer.state = javaContainerState
            javaContainer.name = "java"

        when:
            this.client.inspectContainerCmd("go") >> cmd
            this.client.inspectContainerCmd("go").exec() >> javaContainer

            this.client.inspectContainerCmd("java") >> cmd
            this.client.inspectContainerCmd("java").exec() >> javaContainer
            assertThat(this.client).containers("go", "java").areRunning()

        then:
            AssertionError er = thrown()
            er.message == 'Container java is not running'
    }

    def "container was built over go image"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def containerConfig = new ContainerConfig(image: "go")

            def container = new InspectContainerResponse(config: containerConfig)

        when:
            this.client.inspectContainerCmd("my_go_container") >> cmd
            this.client.inspectContainerCmd("my_go_container").exec() >> container

        then:
            assertThat(this.client).container("my_go_container").hasImage("go")
    }

    def "container was built over go image but should be java"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def containerConfig = new ContainerConfig(image: "go")
            def container = new InspectContainerResponse(config: containerConfig)

        when:
            this.client.inspectContainerCmd("my_go_container") >> cmd
            this.client.inspectContainerCmd("my_go_container").exec() >> container
            assertThat(this.client).container("my_go_container").hasImage("java")

        then:
            AssertionError er = thrown()
            er.message == "Expected container's image name to be java but was go"
    }

    def "container's network mode is bridge"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def hostConfig = new HostConfig(networkMode: "bridge")
            def container = new InspectContainerResponse(hostConfig: hostConfig)

        when:
            this.client.inspectContainerCmd("my_java_container") >> cmd
            this.client.inspectContainerCmd("my_java_container").exec() >> container

        then:
            assertThat(this.client).container("my_java_container").hasNetworkMode("bridge")
    }

    def "container's network mode should be default but it is bridge"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def hostConfig = new HostConfig(networkMode: "bridge")
            def container = new InspectContainerResponse(hostConfig: hostConfig)

        when:
            this.client.inspectContainerCmd("my_java_container") >> cmd
            this.client.inspectContainerCmd("my_java_container").exec() >> container
            assertThat(this.client).container("my_java_container").hasNetworkMode("default")

        then:
            AssertionError er = thrown()
            er.message == "Expected container's networkMode to be default but was bridge"
    }

    def "container has 2 mounts"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def mounts = [new InspectContainerResponse.Mount(), new InspectContainerResponse.Mount()]
            def container = new InspectContainerResponse(mounts: mounts)

        when:
            this.client.inspectContainerCmd("my_java_container") >> cmd
            this.client.inspectContainerCmd("my_java_container").exec() >> container

        then:
            assertThat(this.client).container("my_java_container").hasMountSize(2)
    }

    def "container has 2 mounts but should be 3"() {
        setup:
            InspectContainerCmd cmd = Mock(InspectContainerCmd)

            def mounts = [new InspectContainerResponse.Mount(), new InspectContainerResponse.Mount()]
            def container = new InspectContainerResponse(mounts: mounts)

        when:
            this.client.inspectContainerCmd("my_java_container") >> cmd
            this.client.inspectContainerCmd("my_java_container").exec() >> container
            assertThat(this.client).container("my_java_container").hasMountSize(3)

        then:
            AssertionError er = thrown()
            er.message == "Expected container's mounts size to be 3 but was 2"
    }

}