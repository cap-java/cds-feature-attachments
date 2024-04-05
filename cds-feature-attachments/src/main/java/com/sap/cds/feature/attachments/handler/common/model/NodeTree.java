package com.sap.cds.feature.attachments.handler.common.model;

import java.util.ArrayList;
import java.util.List;

public class NodeTree {

	final AssociationIdentifier identifier;
	final List<NodeTree> children = new ArrayList<>();

	public NodeTree(AssociationIdentifier identifier) {
		this.identifier = identifier;
	}

	public void addPath(List<AssociationIdentifier> path) {
		var currentIdentifierOptional = path.stream()
																																				.filter(entry -> entry.fullEntityName().equals(identifier.fullEntityName()))
																																				.findAny();
		if (currentIdentifierOptional.isEmpty()) {
			return;
		}
		var currentNode = this;
		var index = path.indexOf(currentIdentifierOptional.get());
		if (index == path.size() - 1) {
			return;
		}
		for (var i = index + 1; i < path.size(); i++) {
			var pathEntry = path.get(i);
			currentNode = currentNode.getChildOrNew(pathEntry);
		}
	}

	public NodeTree getChildOrNew(AssociationIdentifier identifier) {
		var childOptional = children.stream()
																								.filter(child -> child.identifier.fullEntityName().equals(identifier.fullEntityName()))
																								.findAny();
		if (childOptional.isPresent()) {
			return childOptional.get();
		} else {
			NodeTree child = new NodeTree(identifier);
			children.add(child);
			return child;
		}
	}

	public AssociationIdentifier getIdentifier() {
		return identifier;
	}

	public List<NodeTree> getChildren() {
		return children;
	}

}
