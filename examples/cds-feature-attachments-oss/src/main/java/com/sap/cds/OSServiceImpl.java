package com.sap.cds;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import com.sap.cds.feature.attachments.service.model.service.AttachmentModificationResult;
import com.sap.cds.feature.attachments.service.model.service.MarkAsDeletedInput;
import com.sap.cds.services.ServiceDelegator;

/**
 * Implementation of the {@link OSService} interface.
 * The main	purpose of this class is to set data in the corresponding context and
 * to call the emit method for the OSService.
 */
public class OSServiceImpl extends ServiceDelegator implements OSService {

	public OSServiceImpl() {
		super(DEFAULT_NAME);
	}

	@Override
	public InputStream readAttachment(String contentId) {
		/*
		var readContext = AttachmentReadEventContext.create();
		readContext.setContentId(contentId);
		readContext.setData(MediaData.create());

		emit(readContext);
		
		return readContext.getData().getContent();
		*/
		String dummyContent = "This is dummy input.";
        InputStream is = new ByteArrayInputStream(dummyContent.getBytes());
		return is;
	}

	@Override
	public AttachmentModificationResult createAttachment(InputStream input) {
		System.out.println("Called createAttachment with input: " + input);
		byte[] b = new byte[1024];
		try {
			input.read(b);
		} catch (IOException e) {
			System.err.println("Error reading input stream: " + e.getMessage());
		}
		/*String osUrl = osCredentials.getUrl() + "browser/" + cmisDocument.getRepositoryId() + "/root";
		HttpPost uploadFile = new HttpPost(osUrl);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addBinaryBody(
			"filename",
			cmisDocument.getContent(),
			ContentType.create(cmisDocument.getMimeType()),
			cmisDocument.getFileName());
		// Add additional form fields
		builder.addTextBody("cmisaction", "createDocument", ContentType.TEXT_PLAIN);
		builder.addTextBody("objectId", cmisDocument.getFolderId(), ContentType.TEXT_PLAIN);
		builder.addTextBody("propertyId[0]", "cmis:name", ContentType.TEXT_PLAIN);
		builder.addTextBody("propertyValue[0]", cmisDocument.getFileName(), ContentType.TEXT_PLAIN);
		builder.addTextBody("propertyId[1]", "cmis:objectTypeId", ContentType.TEXT_PLAIN);
		builder.addTextBody("propertyValue[1]", "cmis:document", ContentType.TEXT_PLAIN);
		builder.addTextBody("succinct", "true", ContentType.TEXT_PLAIN);
		HttpEntity multipart = builder.build();
		uploadFile.setEntity(multipart);
		executeHttpPost(httpClient, uploadFile, cmisDocument, finalResponse);*/
		return new AttachmentModificationResult(true, "dummyContentId", "dummyStatus");
		/*var createContext = AttachmentCreateEventContext.create();
		createContext.setAttachmentIds(input.attachmentIds());
		createContext.setAttachmentEntity(input.attachmentEntity());
		var mediaData = MediaData.create();
		mediaData.setFileName(input.fileName());
		mediaData.setMimeType(input.mimeType());
		mediaData.setContent(input.content());
		createContext.setData(mediaData);

		emit(createContext);

		return new AttachmentModificationResult(Boolean.TRUE.equals(createContext.getIsInternalStored()),
				createContext.getContentId(), createContext.getData().getStatus());*/
	}

	/*private void executeHttpPost(
		HttpClient httpClient,
		HttpPost uploadFile,
		//The actual attachment!
		Map<String, String> finalResponse)
		throws ServiceException {
		try (var response = (CloseableHttpResponse) httpClient.execute(uploadFile)) {
		formResponse(finalResponse, response);
		} catch (IOException e) {
		throw new ServiceException("Error in setting timeout", e.getMessage());
		}
	}

	private void formResponse(
      //CmisDocument cmisDocument,
      Map<String, String> finalResponse,
      CloseableHttpResponse response) {
		String status = "success";
		String name = "name"; //cmisDocument.getFileName();
		String id = "id"; cmisDocument.getAttachmentId();
		String objectId = "";
		String error = "";
		try {
		String responseString = EntityUtils.toString(response.getEntity());
		JSONObject jsonResponse = new JSONObject(responseString);
		int responseCode = response.getStatusLine().getStatusCode();
		if (responseCode == 201 || responseCode == 200) {
			JSONObject succinctProperties = jsonResponse.getJSONObject("succinctProperties");
			status = "success";
			objectId = succinctProperties.getString("cmis:objectId");
		} else {
			String message = jsonResponse.getString("message");
			if (responseCode == 409
				&& "Malware Service Exception: Virus found in the file!".equals(message)) {
			status = "virus";
			} else if (responseCode == 409) {
			status = "duplicate";
			} else if (responseCode == 403) {
			status = "unauthorized";
			} else {
			status = "fail";
			error = message;
			}
		}
		// Construct the final response
		finalResponse.put("name", name);
		finalResponse.put("id", id);
		finalResponse.put("status", status);
		finalResponse.put("message", error);
		if (!objectId.isEmpty()) {
			finalResponse.put("objectId", objectId);
		}
		} catch (IOException e) {
		throw new ServiceException(OSSConstants.getGenericError("upload"));
		}
	}*/


	@Override
	public void markAttachmentAsDeleted(MarkAsDeletedInput input) {
		//var deleteContext = AttachmentMarkAsDeletedEventContext.create();
		//deleteContext.setContentId(input.contentId());
		//deleteContext.setDeletionUserInfo(fillDeletionUserInfo(input.userInfo()));
		//emit(deleteContext);
	}

	@Override
	public void restoreAttachment(Instant restoreTimestamp) {
		//var restoreContext = AttachmentRestoreEventContext.create();
		//restoreContext.setRestoreTimestamp(restoreTimestamp);
		//emit(restoreContext);
	}

	/*private DeletionUserInfo fillDeletionUserInfo(UserInfo userInfo) {
		var deletionUserInfo = DeletionUserInfo.create();
		deletionUserInfo.setName(userInfo.getName());
		return deletionUserInfo;
	}*/

}
