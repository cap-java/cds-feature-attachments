/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The class {@link NodeTree} is a tree data structure that holds the
 * association identifier and its children.
 */
class NodeTree {

	private final AssociationIdentifier identifier;
	private final List<NodeTree> children = new ArrayList<>();

	NodeTree(AssociationIdentifier identifier) {
		this.identifier = identifier;
	}

	void addPath(List<AssociationIdentifier> path) {
		var currentIdentifierOptional = path.stream()
				.filter(entry -> entry.fullEntityName().equals(identifier.fullEntityName())).findAny();
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

	private NodeTree getChildOrNew(AssociationIdentifier identifier) {
		var childOptional = children.stream()
				.filter(child -> child.identifier.fullEntityName().equals(identifier.fullEntityName())).findAny();
		if (childOptional.isPresent()) {
			return childOptional.get();
		} else {
			NodeTree child = new NodeTree(identifier);
			children.add(child);
			return child;
		}
	}

	AssociationIdentifier getIdentifier() {
		return identifier;
	}

	List<NodeTree> getChildren() {
		return Collections.unmodifiableList(children);
	}

	@Override
	public String toString() {
		return "NodeTree{" + "identifier=" + identifier + ", children=" + children + '}';
	}
}
