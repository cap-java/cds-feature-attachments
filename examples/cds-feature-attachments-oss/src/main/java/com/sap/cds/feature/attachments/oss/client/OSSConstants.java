package com.sap.cds.feature.attachments.oss.client;

import java.util.List;
import java.util.Map;

public class OSSConstants {
  private OSSConstants() {
    // Doesn't do anything
  }

  public static final String REPOSITORY_ID = System.getenv("REPOSITORY_ID");
  public static final String SDM_ANNOTATION_ADDITIONALPROPERTY_NAME =
      "SDM.Attachments.AdditionalProperty.name";
  public static final String SDM_ANNOTATION_ADDITIONALPROPERTY =
      "SDM.Attachments.AdditionalProperty";
  public static final String DUPLICATE_FILE_IN_DRAFT_ERROR_MESSAGE =
      "The file(s) %s have been added multiple times. Please rename and try again.";
  public static final String FILES_RENAME_WARNING_MESSAGE =
      "The following files could not be renamed as they already exist:\n%s\n";
  public static final String COULD_NOT_UPDATE_THE_ATTACHMENT = "Could not update the attachment";
  public static final String ATTACHMENT_NOT_FOUND = "Attachment not found";
  public static final String DUPLICATE_FILES_ERROR = "%s already exists.";
  public static final String GENERIC_ERROR = "Could not %s the document.";
  public static final String VERSIONED_REPO_ERROR =
      "Upload not supported for versioned repositories.";
  public static final String VIRUS_ERROR = "%s contains potential malware and cannot be uploaded.";
  public static final String REPOSITORY_ERROR = "Failed to get repository info.";
  public static final String NOT_FOUND_ERROR = "Failed to read document.";
  public static final String NAME_CONSTRAINT_WARNING_MESSAGE =
      "Enter a valid file name for %s. The following characters are not supported: /, \\";
  public static final String SDM_MISSING_ROLES_EXCEPTION_MSG =
      "You do not have the required permissions to update attachments. Kindly contact the admin";
  public static final String SDM_ROLES_ERROR_MESSAGE =
      "Unable to rename the file due to an error at the server";
  public static final String SDM_ENV_NAME = "sdm";

  public static final String SDM_TOKEN_EXCHANGE_DESTINATION = "sdm-token-exchange-flow";
  public static final String SDM_TECHNICAL_CREDENTIALS_FLOW_DESTINATION = "sdm-technical-user-flow";
  public static final String SDM_TOKEN_FETCH = "sdm-token-fetch";
  public static final String SDM_DESTINATION_KEY = "name";
  public static final String SDM_CONNECTIONPOOL_PREFIX = "cds.attachments.sdm.http.%s";
  public static final String USER_NOT_AUTHORISED_ERROR =
      "You do not have the required permissions to upload attachments. Please contact your administrator for access.";
  public static final String FILE_NOT_FOUND_ERROR = "Object not found in repository";
  public static final Integer MAX_CONNECTIONS = 100;
  public static final int CONNECTION_TIMEOUT = 1200;
  public static final int CHUNK_SIZE = 20 * 1024 * 1024; // 20MB Chunk Size
  public static final String ONBOARD_REPO_MESSAGE =
      "Repository with name %s  and id %s onboarded successfully";
  public static final String ONBOARD_REPO_ERROR_MESSAGE =
      "Error in onboarding repository with name %s";
  public static final String UPDATE_ATTACHMENT_ERROR = "Could not update the attachment";
  public static final String ATTACHMENT_MAXCOUNT = "SDM.Attachments.maxCount";
  public static final String ATTACHMENT_MAXCOUNT_ERROR_MSG = "SDM.Attachments.maxCountError";
  public static final String MAX_COUNT_ERROR_MESSAGE =
      "Cannot upload more than %s attachments as set up by the application";
  public static final String NO_SDM_BINDING = "No SDM binding found";
  public static final String DI_TOKEN_EXCHANGE_ERROR = "Error fetching DI token with JWT bearer";
  public static final String DI_TOKEN_EXCHANGE_PARAMS =
      "/oauth/token?grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer";
  public static final String DRAFT_NOT_FOUND = "Attachment draft entity not found";
  public static final String UNSUPPORTED_PROPERTIES = "Unsupported properties";
  public static final String REPOSITORY_VERSIONED = "Versioned";
  public static final Integer TIMEOUT_MILLISECONDS = 900000;
  public static final Integer MAX_CONNECTIONS_PER_ROUTE = 50;
  public static final Integer MAX_CONNECTIONS_TOTAL = 50;
  public static final String REST_V2_REPOSITORIES = "rest/v2/repositories";
  public static final String TECHNICAL_USER_FLOW = "TECHNICAL_CREDENTIALS_FLOW";
  public static final String NAMED_USER_FLOW = "TOKEN_EXCHANGE";
  public static final String ANNOTATION_IS_MEDIA_DATA = "_is_media_data";
  public static final String DRAFT_READONLY_CONTEXT = "DRAFT_READONLY_CONTEXT";

  public static String nameConstraintMessage(
      List<String> fileNameWithRestrictedCharacters, String operation) {
    // Create the base message
    String prefixMessage =
        "%s unsuccessful. The following filename(s) contain unsupported characters (/, \\). \n\n";

    // Create the formatted prefix message
    String formattedPrefixMessage = String.format(prefixMessage, operation);

    // Initialize the StringBuilder with the formatted message prefix
    StringBuilder bulletPoints = new StringBuilder(formattedPrefixMessage);

    // Append each unsupported file name to the StringBuilder
    for (String file : fileNameWithRestrictedCharacters) {
      bulletPoints.append(String.format("\t• %s%n", file));
    }
    bulletPoints.append("\nRename the files and try again.");
    return bulletPoints.toString();
  }

  public static String fileNotFound(List<String> fileNameNotFound) {
    // Create the base message
    String prefixMessage =
        "Update unsuccessful. The following filename(s) could not be updated as they do not exist. \n\n";

    // Create the formatted prefix message
    String formattedPrefixMessage = String.format(prefixMessage);

    // Initialize the StringBuilder with the formatted message prefix
    StringBuilder bulletPoints = new StringBuilder(formattedPrefixMessage);

    // Append each unsupported file name to the StringBuilder
    for (String file : fileNameNotFound) {
      bulletPoints.append(String.format("\t• %s%n", file));
    }
    bulletPoints.append("\nDelete and upload the files again.");
    return bulletPoints.toString();
  }

  public static String badRequestMessage(Map<String, String> badRequest) {
    // Create the base message
    String prefixMessage = "Could not update the following files. \n\n";

    // Initialize the StringBuilder with the formatted message prefix
    StringBuilder bulletPoints = new StringBuilder(prefixMessage);

    // Append each file name and its error message to the StringBuilder
    for (Map.Entry<String, String> entry : badRequest.entrySet()) {
      bulletPoints.append(String.format("\t• %s : %s%n", entry.getKey(), entry.getValue()));
    }
    bulletPoints.append("\nPlease try again.");
    return bulletPoints.toString();
  }

  public static String noSDMRolesMessage(List<String> files, String operation) {
    // Create the base message
    String prefixMessage = "Could not " + operation + " the following files. \n\n";

    // Initialize the StringBuilder with the formatted message prefix
    StringBuilder bulletPoints = new StringBuilder(prefixMessage);

    // Append each file name and its error message to the StringBuilder
    for (String item : files) {
      bulletPoints.append(String.format("\t• %s%n", item));
    }
    bulletPoints.append(System.lineSeparator());
    if (operation.equals("create")) {
      bulletPoints.append(USER_NOT_AUTHORISED_ERROR);
    } else {
      bulletPoints.append(SDM_MISSING_ROLES_EXCEPTION_MSG);
    }

    return bulletPoints.toString();
  }

  public static String unsupportedPropertiesMessage(List<String> propertiesList) {
    // Create the base message
    String prefixMessage = "The following secondary properties are not supported.\n\n";

    // Initialize the StringBuilder with the formatted message prefix
    StringBuilder bulletPoints = new StringBuilder(prefixMessage);

    // Append each unsupported file name to the StringBuilder
    for (String file : propertiesList) {
      bulletPoints.append(String.format("\t• %s%n", file));
    }
    bulletPoints.append(
        "\nPlease contact your administrator for assistance with any necessary adjustments.");
    return bulletPoints.toString();
  }

  public static String getDuplicateFilesError(String filename) {
    return String.format(DUPLICATE_FILES_ERROR, filename);
  }

  public static String getGenericError(String event) {
    return String.format(GENERIC_ERROR, event);
  }

  public static String getVirusFilesError(String filename) {
    return String.format(VIRUS_ERROR, filename);
  }
}
