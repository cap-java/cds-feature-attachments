package com.sap.cds.feature.attachments.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ModelConstantsTest {

    @Test
    void correctValueForMediaDataAnnotation() {
        assertThat(ModelConstants.ANNOTATION_IS_MEDIA_DATA).isEqualTo("IsMediaData");
    }

}
