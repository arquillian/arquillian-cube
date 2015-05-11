package org.arquillian.cube.docker.stub;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;

import java.io.ByteArrayInputStream;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.arquillian.cube.docker.stub.ContainerModel.PortBinding;
import org.arquillian.cube.docker.stub.ContainerModel.Status;

import spark.Request;
import spark.Response;
import spark.Route;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class SparkServer {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String APPLICATION_JSON = "application/json";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String CREATION_RESPONSE = "{\"Id\":\"%s\",\"Warnings\":null}";
    private static final String WAIT_CONTAINER = "{\"StatusCode\":0}";
    private static final String INSPECT_RESPONSE = "{\n" + "  \"Id\":\"\",\n"
            + "  \"Created\":\"2013-05-07T14:51:42.041847+02:00\",\n" + "  \"Path\":\"date\",\n" + "  \"Args\":[\n"
            + "\n" + "  ],\n" + "  \"Config\":{\n" + "    \"Hostname\":\"4fa6e0f0c678\",\n" + "    \"User\":\"\",\n"
            + "    \"Memory\":0,\n" + "    \"MemorySwap\":0,\n" + "    \"AttachStdin\":false,\n"
            + "    \"AttachStdout\":true,\n" + "    \"AttachStderr\":true,\n" + "    \"PortSpecs\":null,\n"
            + "    \"Tty\":false,\n" + "    \"OpenStdin\":false,\n" + "    \"StdinOnce\":false,\n"
            + "    \"Env\":null,\n" + "    \"Cmd\":[\n" + "      \"date\"\n" + "    ],\n" + "    \"Dns\":null,\n"
            + "    \"Image\":\"base\",\n" + "    \"Volumes\":{\n" + "\n" + "    },\n" + "    \"VolumesFrom\":\"\",\n"
            + "    \"WorkingDir\":\"\"\n" + "  },\n" + "  \"State\":{\n" + "    \"Running\":false,\n"
            + "    \"Pid\":0,\n" + "    \"ExitCode\":0,\n"
            + "    \"StartedAt\":\"2013-05-07T14:51:42.087658+02:01360\",\n" + "    \"Ghost\":false\n" + "  },\n"
            + "  \"Image\":\"b750fe79269d2ec9a3c593ef05b4332b1d1a02a62b4accb2c21d589ff2f5f2dc\",\n"
            + "  \"NetworkSettings\":{\n" + "    \"IpAddress\":\"\",\n" + "    \"IpPrefixLen\":0,\n"
            + "    \"Gateway\":\"\",\n" + "    \"Bridge\":\"\",\n" + "    \"PortMapping\":null\n" + "  },\n"
            + "  \"SysInitPath\":\"/home/kitty/go/src/github.com/docker/docker/bin/docker\",\n"
            + "  \"ResolvConfPath\":\"/etc/resolv.conf\",\n" + "  \"Volumes\":{\n" + "\n" + "  },\n"
            + "  \"HostConfig\":{\n" + "    \"Binds\":null,\n" + "    \"ContainerIDFile\":\"\",\n"
            + "    \"LxcConf\":[\n" + "\n" + "    ],\n" + "    \"Privileged\":false,\n" + "    \"PortBindings\":{\n"
            + "    },\n" + "    \"Links\":[\n" + "      \"/name:alias\"\n" + "    ],\n"
            + "    \"PublishAllPorts\":false\n" + "  }\n" + "}";

    private static final String EXPOSED_PORTS = "ExposedPorts";
    private static final String PORT_BINDINGS = "PortBindings";
    private static final String HOST_IP = "HostIp";
    private static final String HOST_PORT = "HostPort";
    private static final String HOST_CONFIG = "HostConfig";
    private static final String ID = "Id";
    private static final String NETWORK_SETTINGS = "NetworkSettings";
    private static final String IP_ADDRESS = "IpAddress";
    private static final String GATEWAY = "Gateway";
    private static final String LOG_LINE = "This is a log line.";

    private Map<String, ContainerModel> containers = new HashMap<>();

    public void start() {

        get("/*/_ping", new Route() {
            public Object handle(Request request, Response response) throws Exception {
                response.type(TEXT_PLAIN);
                response.status(200);

                return "OK";
            }
        });

        post("/*/containers/create", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {

                String id = UUID.randomUUID().toString().replace("-", "");
                ContainerModel containerModel = new ContainerModel(id);

                JsonNode node = mapper.readTree(request.body());
                JsonNode exposedPortsNode = node.get(EXPOSED_PORTS);
                Iterator<String> exposedPorts = exposedPortsNode.fieldNames();
                containerModel.setExposedPorts(toSet(exposedPorts));
                containerModel.setStatus(Status.CREATED);

                registerContainer(containerModel);
                response.type(APPLICATION_JSON);
                response.status(201);
                return String.format(CREATION_RESPONSE, id);
            }
        });

        post("/*/containers/*/start", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                String id = request.splat()[1];
                if (isContainerCreated(id)) {
                    if (isContainerWithOneStatus(id, Status.STARTED)) {
                        response.status(304);
                        response.type(TEXT_PLAIN);
                        return "";
                    } else {
                        ContainerModel container = getContainer(id);
                        JsonNode node = mapper.readTree(request.body());

                        addPortBindingsToContainer(container, node);
                        response.status(204);
                        response.type(TEXT_PLAIN);
                        setStatus(id, Status.STARTED);
                        return "";
                    }
                } else {
                    response.status(404);
                    response.type(TEXT_PLAIN);
                    return "";
                }
            }

            private void addPortBindingsToContainer(ContainerModel container, JsonNode node) {
                JsonNode bindingPortsNode = node.get(PORT_BINDINGS);
                Iterator<String> fieldNames = bindingPortsNode.fieldNames();

                while (fieldNames.hasNext()) {
                    PortBinding portBinding = getPortBinding(bindingPortsNode, fieldNames);
                    container.addPortBinding(portBinding);
                }
            }

            private PortBinding getPortBinding(JsonNode bindingPortsNode, Iterator<String> fieldNames) {
                String exposedPort = fieldNames.next();
                PortBinding portBinding = new PortBinding(exposedPort);
                ArrayNode portBindings = (ArrayNode) bindingPortsNode.get(exposedPort);
                Iterator<JsonNode> hostPortBinding = portBindings.iterator();
                while (hostPortBinding.hasNext()) {
                    portBinding.addPortBinding(hostPortBinding.next().get(HOST_PORT).asText());
                }
                return portBinding;
            }
        });

        post("/*/containers/*/stop", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                String id = request.splat()[1];
                if (isContainerCreated(id) && !isContainerWithOneStatus(id, Status.REMOVED)) {
                    if (isContainerWithOneStatus(id, Status.STOPPED)) {
                        response.status(304);
                        response.type(TEXT_PLAIN);
                        return "";
                    } else {
                        response.status(204);
                        response.type(TEXT_PLAIN);
                        setStatus(id, Status.STOPPED);
                        return "";
                    }
                } else {
                    response.status(404);
                    response.type(TEXT_PLAIN);
                    return "";
                }
            }
        });

        delete("/*/containers/*", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                String id = request.splat()[1];
                if (isContainerCreated(id) && !isContainerWithOneStatus(id, Status.REMOVED)) {
                    response.status(204);
                    response.type(TEXT_PLAIN);
                    setStatus(id, Status.REMOVED);
                    return "";
                } else {
                    response.status(404);
                    response.type(TEXT_PLAIN);
                    return "";
                }
            }
        });

        post("/*/containers/*/wait", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                String id = request.splat()[1];
                if (isContainerCreated(id) && !isContainerWithOneStatus(id, Status.REMOVED)) {
                    response.type(APPLICATION_JSON);
                    response.status(200);

                    return WAIT_CONTAINER;
                } else {
                    response.status(404);
                    response.type(TEXT_PLAIN);
                    return "";
                }
            }
        });

        post("/*/images/create", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                String image = request.queryParams("fromImage");
                response.type(APPLICATION_JSON);
                response.status(200);
                return "{\"status\":\"Pulling..." + image + "\"}";
            }
        });

        post("*/build", new Route() {

            @Override
            public Object handle(Request request, Response response) throws Exception {
                response.type(APPLICATION_JSON);
                response.status(200);
                return "{\"status\":\"Successfully built " + UUID.randomUUID().toString().replace("-", "") + "\"}";
            }
        });

        post("/*/containers/*/copy", new Route() {

            @Override
            public Object handle(Request request, Response response) throws Exception {
                String id = request.splat()[1];
                if (isContainerCreated(id) && isContainerWithOneStatus(id, Status.STARTED)) {
                    response.status(200);
                    response.type("application/x-tar");
                    return SparkServer.class.getResourceAsStream("/test.tar");
                } else {
                    response.status(404);
                    response.type(TEXT_PLAIN);
                    return "";
                }
            }
        });

        get("/*/containers/*/logs", new Route() {

            @Override
            public Object handle(Request request, Response response) throws Exception {
                // A bug on spark makes this not to work, going to send a PR to spark
                String id = request.splat()[1];
                if (isContainerCreated(id) && isContainerWithOneStatus(id, Status.STARTED)) {

                    byte[] buffer = LOG_LINE.getBytes();
                    byte[] header = createHeader(buffer);

                    byte[] stream = new byte[header.length + buffer.length];
                    System.arraycopy(header, 0, stream, 0, header.length);
                    System.arraycopy(buffer, 0, stream, header.length, buffer.length);
                    response.status(200);
                    response.type("application/vnd.docker.raw-stream");
                    return stream;
                } else {
                    response.status(404);
                    response.type(TEXT_PLAIN);
                    return "";
                }
            }

            private byte[] createHeader(byte[] buffer) {
                byte[] header = new byte[8];
                header[0] = 1;
                header[1] = 0;
                header[2] = 0;
                header[3] = 0;

                ByteBuffer b = ByteBuffer.allocate(4);
                b.order(ByteOrder.BIG_ENDIAN);
                b.putInt(buffer.length);
                byte[] result = b.array();
                header[4] = result[0];
                header[5] = result[1];
                header[6] = result[2];
                header[7] = result[3];

                return header;
            }
        });

        get("/*/containers/*/json", new Route() {
            @Override
            public Object handle(Request request, Response response) throws Exception {
                // A bug on spark makes this not to work, going to send a PR to spark
                String id = request.splat()[1];
                if (isContainerCreated(id) && isContainerWithOneStatus(id, Status.STARTED)) {
                    ObjectNode node = (ObjectNode) mapper.readTree(INSPECT_RESPONSE);

                    updateId(id, node);
                    updatePortBindings(id, node);
                    ObjectNode networkSettings = (ObjectNode) node.get(NETWORK_SETTINGS);
                    networkSettings.replace(IP_ADDRESS, TextNode.valueOf(Inet4Address.getLocalHost().getHostAddress()));
                    networkSettings.replace(GATEWAY, TextNode.valueOf(Inet4Address.getLocalHost().getHostAddress()));
                    response.type(APPLICATION_JSON);
                    response.status(200);
                    return node;
                } else {
                    response.status(404);
                    response.type(TEXT_PLAIN);
                    return "";
                }
            }

            private void updateId(String id, ObjectNode node) {
                node.replace(ID, TextNode.valueOf(id));
            }

            private void updatePortBindings(String id, ObjectNode node) {
                JsonPointer portBindingsPointer = JsonPointer.compile("/" + HOST_CONFIG + "/" + PORT_BINDINGS);
                ObjectNode portBindings = (ObjectNode) node.at(portBindingsPointer);
                ContainerModel container = getContainer(id);

                Set<PortBinding> portBindingsSet = container.getPortBindings();
                for (PortBinding portBinding : portBindingsSet) {
                    portBindings.setAll(createPort(portBinding));
                }
            }
        });
    }

    public boolean isContainerWithOneStatus(String id, Status... status) {
        synchronized (containers) {
            ContainerModel container = getContainer(id);
            if (container != null) {
                for (Status stat : status) {
                    if (stat == container.getStatus()) {
                        return true;
                    }
                }
            } else {
                return false;
            }
        }

        return false;
    }

    public void setStatus(String id, Status status) {
        synchronized (containers) {
            ContainerModel containerModel = containers.get(id);
            if (containerModel != null) {
                containerModel.setStatus(status);
            }
        }
    }

    public boolean isContainerCreated(String id) {
        synchronized (containers) {
            return containers.containsKey(id);
        }
    }

    public ContainerModel getContainer(String id) {
        synchronized (containers) {
            return containers.get(id);
        }
    }

    public void registerContainer(ContainerModel containerModel) {
        synchronized (containers) {
            containers.put(containerModel.getId(), containerModel);
        }
    }

    private ObjectNode createPort(PortBinding portBinding) {

        ArrayNode portsNode = mapper.createArrayNode();

        Set<String> portBindings = portBinding.getPortBindings();
        for (String hostPort : portBindings) {
            ObjectNode portBindingNode = mapper.createObjectNode();
            portBindingNode.put(HOST_IP, "0.0.0.0");
            portBindingNode.put(HOST_PORT, hostPort);
            portsNode.add(portBindingNode);
        }

        ObjectNode portNode = mapper.createObjectNode();
        portNode.set(portBinding.getExposedPort(), portsNode);

        return portNode;
    }

    private Set<String> toSet(Iterator<String> iterator) {
        Set<String> set = new HashSet<>();

        while (iterator.hasNext()) {
            set.add(iterator.next());
        }
        return set;
    }
}
