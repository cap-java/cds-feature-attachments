package com.sap.cds.feature.attachments.service.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class AutoConfigurationTest {

		private AutoConfiguration cut;

		@BeforeEach
		void setup() {
				cut = new AutoConfiguration();
		}

		@Test
		void instanceCreated() {
				assertThat(cut.buildAttachmentService()).isNotNull();
		}

		@Test
		void correctAnnotationForClass() {
				assertThat(cut.getClass().getAnnotation(Configuration.class)).isNotNull();
		}

		@Test
		void correctAnnotationUsedForAttachmentServiceMethod() throws NoSuchMethodException {
				var beanAnnotation = cut.getClass().getMethod("buildAttachmentService").getAnnotation(Bean.class);
				assertThat(beanAnnotation).isNotNull();
		}

}
