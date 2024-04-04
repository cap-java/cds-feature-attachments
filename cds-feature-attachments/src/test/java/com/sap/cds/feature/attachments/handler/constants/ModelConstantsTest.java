package com.sap.cds.feature.attachments.handler.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModelConstantsTest {

	@Test
	void correctValueForMediaDataAnnotation() {
		assertThat(ModelConstants.ANNOTATION_IS_MEDIA_DATA).isEqualTo("_is_media_data");
	}

	@Test
	void correctValueForMediaTypeAnnotation() {
		assertThat(ModelConstants.ANNOTATION_CORE_MEDIA_TYPE).isEqualTo("Core.MediaType");
	}

}
