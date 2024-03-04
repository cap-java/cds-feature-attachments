package com.sap.cds.feature.attachments.handler.processor.applicationevents.modifier;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultItemModifierProviderTest {

	private DefaultItemModifierProvider cut;

	@BeforeEach
	void setup() {
		cut = new DefaultItemModifierProvider();
	}

	@Test
	void correctInstanceReturned() {
		var instance = cut.getBeforeReadDocumentIdEnhancer(Collections.emptyMap());

		assertThat(instance).isInstanceOf(BeforeReadItemsModifier.class);
	}

}
