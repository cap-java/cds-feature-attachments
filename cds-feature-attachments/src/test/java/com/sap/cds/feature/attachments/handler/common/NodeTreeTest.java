package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class NodeTreeTest {

	@ParameterizedTest
	@ValueSource(strings = {"associationName"})
	@EmptySource
	@NullSource
	void singlePathEntryWorksCorrectly(String associationName) {
		AssociationIdentifier identifier = new AssociationIdentifier(associationName, "fullEntityName");

		var cut = new NodeTree(identifier);
		cut.addPath(List.of(identifier));

		assertThat(cut.getChildren()).isEmpty();
	}

	@Test
	void addPathWithComplexPathTable() {
		AssociationIdentifier identifier1 = new AssociationIdentifier("", "fullEntityName1");
		AssociationIdentifier identifier2 = new AssociationIdentifier("associationName2", "fullEntityName2");
		AssociationIdentifier identifier3 = new AssociationIdentifier("associationName3", "fullEntityName3");

		var cut = new NodeTree(identifier1);
		cut.addPath(List.of(identifier1, identifier2, identifier3));

		verifyTwoNodeHierarchies(cut, identifier2, identifier3);
	}

	@Test
	void addPathWithComplexPathTableAndExistingPath() {
		AssociationIdentifier identifier1 = new AssociationIdentifier("", "fullEntityName1");
		AssociationIdentifier identifier2 = new AssociationIdentifier("associationName2", "fullEntityName2");
		AssociationIdentifier identifier3 = new AssociationIdentifier("associationName3", "fullEntityName3");

		var cut = new NodeTree(identifier1);
		cut.addPath(List.of(identifier1, identifier2, identifier3));
		cut.addPath(List.of(identifier1, identifier2, identifier3));

		verifyTwoNodeHierarchies(cut, identifier2, identifier3);
	}

	@Test
	void addComplexPathWithMultipleChildren() {
		AssociationIdentifier identifier1 = new AssociationIdentifier("", "fullEntityName1");
		AssociationIdentifier identifier2 = new AssociationIdentifier("associationName2", "fullEntityName2");
		AssociationIdentifier identifier3 = new AssociationIdentifier("associationName3", "fullEntityName3");
		AssociationIdentifier identifier4 = new AssociationIdentifier("associationName4", "fullEntityName4");

		var cut = new NodeTree(identifier1);
		cut.addPath(List.of(identifier1, identifier2));
		cut.addPath(List.of(identifier1, identifier3));
		cut.addPath(List.of(identifier1, identifier2, identifier3));
		cut.addPath(List.of(identifier1, identifier3, identifier4));
		cut.addPath(List.of(identifier1, identifier4, identifier2, identifier3));
		cut.addPath(List.of(identifier1, identifier4, identifier4));

		assertThat(cut.getChildren()).hasSize(3);
		assertThat(cut.getChildren().get(0).getIdentifier()).isEqualTo(identifier2);
		assertThat(cut.getChildren().get(0).getChildren()).hasSize(1);
		assertThat(cut.getChildren().get(0).getChildren().get(0).getIdentifier()).isEqualTo(identifier3);
		assertThat(cut.getChildren().get(0).getChildren().get(0).getChildren()).isNotNull().isEmpty();

		assertThat(cut.getChildren().get(1).getIdentifier()).isEqualTo(identifier3);
		assertThat(cut.getChildren().get(1).getChildren()).hasSize(1);
		assertThat(cut.getChildren().get(1).getChildren().get(0).getIdentifier()).isEqualTo(identifier4);
		assertThat(cut.getChildren().get(1).getChildren().get(0).getChildren()).isNotNull().isEmpty();

		assertThat(cut.getChildren().get(2).getIdentifier()).isEqualTo(identifier4);
		assertThat(cut.getChildren().get(2).getChildren()).hasSize(2);
		assertThat(cut.getChildren().get(2).getChildren().get(0).getIdentifier()).isEqualTo(identifier2);
		assertThat(cut.getChildren().get(2).getChildren().get(0).getChildren()).hasSize(1);
		assertThat(cut.getChildren().get(2).getChildren().get(0).getChildren().get(0).getIdentifier()).isEqualTo(identifier3);
		assertThat(cut.getChildren().get(2).getChildren().get(0).getChildren().get(0).getChildren()).isNotNull().isEmpty();

		assertThat(cut.getChildren().get(2).getChildren().get(1).getIdentifier()).isEqualTo(identifier4);
		assertThat(cut.getChildren().get(2).getChildren().get(1).getChildren()).isNotNull().isEmpty();
	}

	@Test
	void nodeNotInPathWillDoNothing() {
		AssociationIdentifier identifier1 = new AssociationIdentifier("", "fullEntityName1");
		AssociationIdentifier identifier2 = new AssociationIdentifier("associationName2", "fullEntityName2");
		AssociationIdentifier identifier3 = new AssociationIdentifier("associationName3", "fullEntityName3");

		var cut = new NodeTree(identifier1);
		cut.addPath(List.of(identifier2, identifier3));

		assertThat(cut.getChildren()).isEmpty();
	}

	private void verifyTwoNodeHierarchies(NodeTree cut, AssociationIdentifier identifier2,
			AssociationIdentifier identifier3) {
		assertThat(cut.getChildren()).hasSize(1);
		assertThat(cut.getChildren().get(0).getIdentifier()).isEqualTo(identifier2);
		assertThat(cut.getChildren().get(0).getChildren()).hasSize(1);
		assertThat(cut.getChildren().get(0).getChildren().get(0).getIdentifier()).isEqualTo(identifier3);
		assertThat(cut.getChildren().get(0).getChildren().get(0).getChildren()).isEmpty();
	}

}
