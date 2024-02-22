package com.sap.cds.feature.attachments.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ModelConstantsTest {

    @Test
    void correctValueForMediaDataAnnotation() {
        assertThat(ModelConstants.ANNOTATION_IS_MEDIA_DATA).isEqualTo("_is_media_data");
    }

    @Test
    void correctValueForDocumentIdAnnotation() {
        assertThat(ModelConstants.ANNOTATION_IS_EXTERNAL_DOCUMENT_ID).isEqualTo("_is_document_id");
    }

		@Test
		void correctValueForMediaTypeAnnotation() {
				assertThat(ModelConstants.ANNOTATION_MEDIA_TYPE).isEqualTo("Core.MediaType");
		}

}
