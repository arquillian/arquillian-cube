package org.arquillian.cube.spi;

import java.util.HashSet;
import java.util.Set;

/**
 * Node is a node in a tree/graph structure. It is used to draw the graph dependencies of each containers so they can be started in the correct order and in case it is possible in parallel.
 */
public class Node {

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

    public String nodeList(Set<Node> nodes) {
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
