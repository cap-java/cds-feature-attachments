/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Roots_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.EventItems_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.services.runtime.CdsRuntime;

class AssociationCascaderTest {

	private static CdsRuntime runtime;
	private AssociationCascader cut;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		cut = new AssociationCascader();
	}

	@Test
	void pathCorrectFoundForRoot() {
		var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);

		var rootNode = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(rootNode.getIdentifier().associationName()).isEmpty();
		assertThat(rootNode.getIdentifier().fullEntityName()).isEqualTo(RootTable_.CDS_NAME);
		var rootChildren = rootNode.getChildren();
		assertThat(rootChildren).hasSize(2);
		var rootAttachmentNode = rootChildren.get(0);
		assertThat(rootAttachmentNode.getChildren()).isNotNull().isEmpty();
		assertThat(rootAttachmentNode.getIdentifier().associationName()).isEqualTo(RootTable.ATTACHMENTS);
		assertThat(rootAttachmentNode.getIdentifier().fullEntityName()).isEqualTo(
				"unit.test.TestService.RootTable.attachments");

		var itemNode = rootChildren.get(1);
		assertThat(itemNode.getIdentifier().associationName()).isEqualTo(RootTable.ITEM_TABLE);
		assertThat(itemNode.getIdentifier().fullEntityName()).isEqualTo(Items_.CDS_NAME);
		assertThat(itemNode.getChildren()).hasSize(2);
		verifyItemAttachments(itemNode, Attachment_.CDS_NAME, "unit.test.TestService.Items.itemAttachments");
	}

	@Test
	void pathCorrectFoundForItem() {
		var serviceEntity = runtime.getCdsModel().findEntity(Items_.CDS_NAME);

		var itemNode = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(itemNode.getIdentifier().associationName()).isEmpty();
		assertThat(itemNode.getIdentifier().fullEntityName()).isEqualTo(Items_.CDS_NAME);
		assertThat(itemNode.getChildren()).hasSize(2);
		verifyItemAttachments(itemNode, Attachment_.CDS_NAME, "unit.test.TestService.Items.itemAttachments");
	}

	@Test
	void noPathFoundForEvents() {
		var serviceEntity = runtime.getCdsModel().findEntity(Events_.CDS_NAME);

		var eventNode = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(eventNode.getIdentifier().associationName()).isEmpty();
		assertThat(eventNode.getIdentifier().fullEntityName()).isEqualTo(Events_.CDS_NAME);
		assertThat(eventNode.getChildren()).isNotNull().isEmpty();
	}

	@Test
	void pathCorrectFoundForDatabaseRoots() {
		var serviceEntity = runtime.getCdsModel().findEntity(Roots_.CDS_NAME);

		var databaseRootNode = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(databaseRootNode.getIdentifier().associationName()).isEmpty();
		assertThat(databaseRootNode.getIdentifier().fullEntityName()).isEqualTo(Roots_.CDS_NAME);
		assertThat(databaseRootNode.getChildren()).hasSize(2);
		var databaseRootAttachmentNode = databaseRootNode.getChildren().get(0);
		assertThat(databaseRootAttachmentNode.getIdentifier().associationName()).isEqualTo("attachments");
		assertThat(databaseRootAttachmentNode.getIdentifier().fullEntityName()).isEqualTo("unit.test.Roots.attachments");

		var databaseRootItemNode = databaseRootNode.getChildren().get(1);
		assertThat(databaseRootItemNode.getIdentifier().associationName()).isEqualTo("itemTable");
		assertThat(databaseRootItemNode.getIdentifier().fullEntityName()).isEqualTo("unit.test.Items");
		verifyItemAttachments(databaseRootItemNode,
				com.sap.cds.feature.attachments.generated.test.cds4j.unit.test.Attachment_.CDS_NAME,
				"unit.test.Items.itemAttachments");
	}

	@Test
	void noPathFoundForEventItems() {
		var serviceEntity = runtime.getCdsModel().findEntity(EventItems_.CDS_NAME);

		var eventItems = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(eventItems.getChildren()).isNotNull().isEmpty();
	}

	private void verifyItemAttachments(NodeTree itemNode, String attachmentNodeName, String itemAttachmentNodeName) {
		var attachmentNode = itemNode.getChildren().get(0);
		assertThat(attachmentNode.getIdentifier().associationName()).isEqualTo(Items.ATTACHMENTS);
		assertThat(attachmentNode.getIdentifier().fullEntityName()).isEqualTo(attachmentNodeName);
		assertThat(attachmentNode.getChildren()).isNotNull().isEmpty();
		var itemAttachmentNode = itemNode.getChildren().get(1);
		assertThat(itemAttachmentNode.getIdentifier().associationName()).isEqualTo("itemAttachments");
		assertThat(itemAttachmentNode.getIdentifier().fullEntityName()).isEqualTo(itemAttachmentNodeName);
		assertThat(itemAttachmentNode.getChildren()).isNotNull().isEmpty();
	}

}
