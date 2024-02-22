package com.sap.cds.feature.attachments.service.configuration;

import com.sap.cds.feature.attachments.service.DummyAttachmentsService;
import com.sap.cds.services.runtime.CdsRuntimeConfiguration;
import com.sap.cds.services.runtime.CdsRuntimeConfigurer;

public class AttachmentServiceConfiguration implements CdsRuntimeConfiguration {

		@Override
		public void services(CdsRuntimeConfigurer configurer) {
				configurer.service(new DummyAttachmentsService());
		}

}
