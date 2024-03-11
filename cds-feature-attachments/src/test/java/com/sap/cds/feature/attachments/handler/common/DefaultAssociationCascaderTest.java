package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.EventItems_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Events;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Events_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Roots_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Attachment_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Items;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.Items_;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable;
import com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.testservice.RootTable_;
import com.sap.cds.feature.attachments.handler.helper.RuntimeHelper;
import com.sap.cds.services.runtime.CdsRuntime;

class DefaultAssociationCascaderTest {

	private static CdsRuntime runtime;
	private DefaultAssociationCascader cut;

	@BeforeAll
	static void classSetup() {
		runtime = RuntimeHelper.runtime;
	}

	@BeforeEach
	void setup() {
		cut = new DefaultAssociationCascader();
	}

	@Test
	void pathCorrectFoundForRoot() {
		var serviceEntity = runtime.getCdsModel().findEntity(RootTable_.CDS_NAME);

		var pathList = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(pathList).hasSize(2);

		var rootPath = pathList.get(0);
		assertThat(rootPath).hasSize(2);
		assertThat(rootPath.get(0).associationName()).isEmpty();
		assertThat(rootPath.get(0).fullEntityName()).isEqualTo(RootTable_.CDS_NAME);
		assertThat(rootPath.get(0).isMediaType()).isFalse();
		assertThat(rootPath.get(1).associationName()).isEqualTo(RootTable.ATTACHMENT_TABLE);
		assertThat(rootPath.get(1).fullEntityName()).isEqualTo("unit.test.TestService.RootTable.attachments");
		assertThat(rootPath.get(1).isMediaType()).isTrue();

		var itemPath = pathList.get(1);
		assertThat(itemPath).hasSize(3);
		assertThat(itemPath.get(0).associationName()).isEmpty();
		assertThat(itemPath.get(0).fullEntityName()).isEqualTo(RootTable_.CDS_NAME);
		assertThat(itemPath.get(0).isMediaType()).isFalse();
		assertThat(itemPath.get(1).associationName()).isEqualTo(RootTable.ITEMS);
		assertThat(itemPath.get(1).fullEntityName()).isEqualTo(Items_.CDS_NAME);
		assertThat(itemPath.get(1).isMediaType()).isFalse();
		assertThat(itemPath.get(2).associationName()).isEqualTo(Items.ATTACHMENTS);
		assertThat(itemPath.get(2).fullEntityName()).isEqualTo(Attachment_.CDS_NAME);
		assertThat(itemPath.get(2).isMediaType()).isTrue();
	}

	@Test
	void pathCorrectFoundForItem() {
		var serviceEntity = runtime.getCdsModel().findEntity(Items_.CDS_NAME);

		var pathList = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(pathList).hasSize(1);

		var itemPath = pathList.get(0);
		assertThat(itemPath).hasSize(2);
		assertThat(itemPath.get(0).associationName()).isEmpty();
		assertThat(itemPath.get(0).fullEntityName()).isEqualTo(Items_.CDS_NAME);
		assertThat(itemPath.get(0).isMediaType()).isFalse();
		assertThat(itemPath.get(1).associationName()).isEqualTo(Items.ATTACHMENTS);
		assertThat(itemPath.get(1).fullEntityName()).isEqualTo(Attachment_.CDS_NAME);
		assertThat(itemPath.get(1).isMediaType()).isTrue();
	}

	@Test
	void pathCorrectFoundForEvents() {
		var serviceEntity = runtime.getCdsModel().findEntity(Events_.CDS_NAME);

		var pathList = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(pathList).hasSize(1);

		var itemPath = pathList.get(0);
		assertThat(itemPath).hasSize(3);
		assertThat(itemPath.get(0).associationName()).isEmpty();
		assertThat(itemPath.get(0).fullEntityName()).isEqualTo(Events_.CDS_NAME);
		assertThat(itemPath.get(0).isMediaType()).isFalse();
		assertThat(itemPath.get(1).associationName()).isEqualTo(Events.ITEMS_COMPO);
		assertThat(itemPath.get(1).fullEntityName()).isEqualTo(com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Items_.CDS_NAME);
		assertThat(itemPath.get(1).isMediaType()).isFalse();
		assertThat(itemPath.get(2).associationName()).isEqualTo(Items.ATTACHMENTS);
		assertThat(itemPath.get(2).fullEntityName()).isEqualTo(com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Attachment_.CDS_NAME);
		assertThat(itemPath.get(2).isMediaType()).isTrue();
	}

	@Test
	void pathCorrectFoundForDatabaseRoots() {
		var serviceEntity = runtime.getCdsModel().findEntity(Roots_.CDS_NAME);

		var pathList = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(pathList).hasSize(2);

		var rootPath = pathList.get(0);
		assertThat(rootPath).hasSize(2);
		assertThat(rootPath.get(0).associationName()).isEmpty();
		assertThat(rootPath.get(0).fullEntityName()).isEqualTo(Roots_.CDS_NAME);
		assertThat(rootPath.get(0).isMediaType()).isFalse();
		assertThat(rootPath.get(1).associationName()).isEqualTo("attachments");
		assertThat(rootPath.get(1).fullEntityName()).isEqualTo("unit.test.Roots.attachments");
		assertThat(rootPath.get(1).isMediaType()).isTrue();

		var itemPath = pathList.get(1);
		assertThat(itemPath).hasSize(3);
		assertThat(itemPath.get(0).associationName()).isEmpty();
		assertThat(itemPath.get(0).fullEntityName()).isEqualTo(Roots_.CDS_NAME);
		assertThat(itemPath.get(0).isMediaType()).isFalse();
		assertThat(itemPath.get(1).associationName()).isEqualTo("itemTable");
		assertThat(itemPath.get(1).fullEntityName()).isEqualTo(com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Items_.CDS_NAME);
		assertThat(itemPath.get(1).isMediaType()).isFalse();
		assertThat(itemPath.get(2).associationName()).isEqualTo("attachments");
		assertThat(itemPath.get(2).fullEntityName()).isEqualTo(com.sap.cds.feature.attachments.generation.test.cds4j.unit.test.Attachment_.CDS_NAME);
		assertThat(itemPath.get(2).isMediaType()).isTrue();
	}

	@Test
	void noPathFoundForEventItems() {
		var serviceEntity = runtime.getCdsModel().findEntity(EventItems_.CDS_NAME);

		var pathList = cut.findEntityPath(runtime.getCdsModel(), serviceEntity.orElseThrow());

		assertThat(pathList).isEmpty();
	}

}
