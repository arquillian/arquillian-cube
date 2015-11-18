package org.arquillian.cube.docker.impl.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.arquillian.cube.docker.impl.client.AutoStartParser;
import org.arquillian.cube.docker.impl.client.CubeDockerConfiguration;

public class AutoStartOrderUtil {

    public static final String REGEXP = "regexp:";

    public static List<String[]> getAutoStopOrder(CubeDockerConfiguration config) {
        List<String[]> autoStartOrder = getAutoStartOrder(config);
        Collections.reverse(autoStartOrder);
        return autoStartOrder;
    }

    public static List<String[]> getAutoStartOrder(CubeDockerConfiguration config) {
        List<String[]> sorted = new ArrayList<>();
        List<Step> steps = sort(from(config));
        for(Step step : steps) {
            sorted.add(step.getIDs());
        }
        return sorted;
    }

    static List<Step> sort(Set<Node> nodes) {
        List<Step> steps = new ArrayList<>();

        List<Node> remaining = new ArrayList<>(nodes);
        int previousSize = remaining.size();
        while(!remaining.isEmpty()) {
            Step step = new Step();
            for(int i = 0; i < remaining.size(); i++) {
                Node node = remaining.get(i);
                if(!node.hasParent() || nodesInStep(steps, node.getParents())) {
                    step.add(node);
                    remaining.remove(i);
                    --i;
                }
            }
            if(previousSize == remaining.size()) {
                throw new IllegalArgumentException("Could not resolve autoStart order. " + nodes);
            }
            previousSize = remaining.size();
            steps.add(step);
        }
        return steps;
    }

    static Set<Node> from(CubeDockerConfiguration config) {
        Map<String, Node> nodes = new HashMap<>();

        AutoStartParser autoStartParser = config.getAutoStartContainers();
        if(autoStartParser != null) {
            nodes.putAll(autoStartParser.parse());
        }

        // add all children links
        Map<String, Node> autoStartNodes = new HashMap<>(nodes);
        for(Map.Entry<String, Node> node : autoStartNodes.entrySet()) {
            addAll(nodes, config, node.getKey());
        }

        return new HashSet<>(nodes.values());
    }

    private static boolean nodesInStep(List<Step> steps, Set<Node> nodes) {
        for(Node node: nodes) {
            if(!nodeInStep(steps, node)) {
                return false;
            }
        }
        return true;
    }

    private static boolean nodeInStep(List<Step> steps, Node node) {
        for(Step step : steps) {
            if(step.contains(node)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void addAll(Map<String, Node> nodes, CubeDockerConfiguration config, String id) {
        Map<String, Object> content = (Map<String, Object>)config.getDockerContainersContent().get(id);
        if(content == null) {
            return;
        }
        Node parent = nodes.get(id);
        if(content.containsKey("links")) {
            List<String> links = (List<String>)content.get("links");
            for(String link : links) {
                String[] parsed = link.split(":");
                String name = parsed[0];

                if(config.getDockerContainersContent().containsKey(name)) {
                    Node child = nodes.get(name);
                    if(child == null) {
                        child = Node.from(name);
                        nodes.put(name, child);
                    }
                    // Only continue recursively if this was a new found child
                    if(child.addAsChildOf(parent)) {
                        addAll(nodes, config, name);
                    }
                }
            }
        }
    }

    public static class Node {
        private String id;
        private Set<Node> parents;
        private Set<Node> children;

        private Node(String id) {
            this.id = id;
            this.parents = new HashSet<>();
            this.children = new HashSet<>();
        }

        public String getId() {
            return id;
        }

        public boolean addAsParentOf(Node node) {
            if(!this.parents.contains(node)) {
                this.parents.add(node);
                node.addAsChildOf(this);
                return true;
            }
            return false;
        }

        public boolean addAsChildOf(Node node) {
            if(!this.children.contains(node)) {
                this.children.add(node);
                node.addAsParentOf(this);
                return true;
            }
            return false;
        }

        public Set<Node> getParents() {
            return parents;
        }

        public boolean hasParent() {
            return this.parents.size() > 0;
        }

        public static Node from(String id) {
            return new Node(id);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Node [id=" + id);
            if(!parents.isEmpty()) {
                sb.append(", parents=" + nodeList(parents));
            }
            if(!children.isEmpty()) {
                sb.append(", children="+ nodeList(children));
            }
            sb.append("]");
            return sb.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Node other = (Node) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }
    }

    public static class Step {
        private Set<Node> nodes;

        private Step() {
            this.nodes = new HashSet<>();
        }

        public boolean contains(Node node) {
            return this.nodes.contains(node);
        }

        public void add(Node node) {
            if(!this.nodes.contains(node)) {
                this.nodes.add(node);
            }
        }

        public String[] getIDs() {
            String[] ids = new String[this.nodes.size()];
            Node[] nodes = this.nodes.toArray(new Node[]{});
            for(int i = 0; i < nodes.length; i++) {
                ids[i] = nodes[i].getId();
            }
            return ids;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Step [ids=" + nodeList(nodes));
            sb.append("]");
            return sb.toString();
        }
    }

    private static String nodeList(Set<Node> nodes) {
        StringBuilder sb = new StringBuilder();
        Node[] array = nodes.toArray(new Node[]{});
        for(int i = 0; i < array.length; i++) {
            sb.append(array[i].getId());
            if(i < array.length-1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public static String toString(Map<String, Node> nodeMap) {
        return nodeList(new HashSet<Node>(nodeMap.values()));
    }
}
