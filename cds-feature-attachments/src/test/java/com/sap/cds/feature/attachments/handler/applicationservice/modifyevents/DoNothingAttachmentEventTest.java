/*
 * © 2024-2025 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.modifyevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sap.cds.feature.attachments.generated.cds4j.sap.attachments.Attachments;
import com.sap.cds.ql.cqn.Path;
import com.sap.cds.ql.cqn.ResolvedSegment;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.EventContext;

class DoNothingAttachmentEventTest {
	private DoNothingAttachmentEvent cut;

	@BeforeEach
	void setup() {
		cut = new DoNothingAttachmentEvent();
	}

	@ParameterizedTest
	@ValueSource(strings = {"test content"})
	@NullSource
	@EmptySource
	void contentReturned(String input) {
		var streamInput = Objects.nonNull(input) ? new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)) : null;
		var path = mock(Path.class);
		var element = mock(CdsElement.class);
		var data = mock(Attachments.class);
		var target = mock(ResolvedSegment.class);
		when(path.target()).thenReturn(target);
		var entity = mock(CdsEntity.class);
		when(target.entity()).thenReturn(entity);
		when(entity.getQualifiedName()).thenReturn("some.qualified.name");

		var result = cut.processEvent(path, streamInput, data, mock(EventContext.class));

		assertThat(result).isEqualTo(streamInput);
		verifyNoInteractions(element, data);
	}

}
