/*
 * © 2026 SAP SE or an SAP affiliate company and cds-feature-attachments contributors.
 */
package com.sap.cds.feature.attachments.handler.applicationservice.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.sap.cds.CdsData;
import com.sap.cds.CdsDataProcessor;
import com.sap.cds.CdsDataProcessor.Filter;
import com.sap.cds.CdsDataProcessor.Validator;
import com.sap.cds.feature.attachments.handler.applicationservice.helper.media.MediaTypeResolver;
import com.sap.cds.feature.attachments.handler.common.ApplicationHandlerHelper;
import com.sap.cds.reflect.CdsAnnotation;
import com.sap.cds.reflect.CdsAssociationType;
import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.reflect.CdsType;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

class AttachmentDataExtractorTest {

  @Mock private CdsElement attachmentElement;
  @Mock private CdsAssociationType associationType;
  @Mock private CdsEntity targetEntity;
  @Mock private CdsType cdsType;
  @Mock private CdsDataProcessor processor;
  @Mock private CdsAnnotation<Object> annotation;
  private MockedStatic<MediaTypeResolver> mediaMock;
  private MockedStatic<ApplicationHandlerHelper> helperMock;
  private MockedStatic<CdsDataProcessor> processorMock;

  private static final String FILE_NAME = "fileName";
  private static final String ATTACHMENT_ENTITY = "test.Attachment";
  private static final String ATTACHMENT_FIELD = "mediaValidatedAttachments";

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    initStaticMocks();
    setupAttachmentModel();
  }

  @AfterEach
  void tearDown() {
    mediaMock.close();
    helperMock.close();
    processorMock.close();
  }

  @Test
  void shouldReturnFileName_whenValidAttachmentProvided() {
    // Arrange
    CdsData cdsData = prepareCdsDataWithAttachments("test.jpeg");

    // Act
    Map<String, Set<String>> result = extractFileNames(cdsData);

    // Assert
    assertThat(result).containsKey(ATTACHMENT_ENTITY);
    assertThat(result.get(ATTACHMENT_ENTITY)).contains("test.jpeg");
  }

  @Test
  void extractFileNames_whenFilenameBlank_throwsBadRequest() {
    // Arrange
    CdsData cdsData = prepareCdsDataWithAttachments(" ");

    // Act
    ServiceException ex = assertThrows(ServiceException.class, () -> extractFileNames(cdsData));

    // Assert
    assertThat(ex.getErrorStatus()).isEqualTo(ErrorStatuses.BAD_REQUEST);
  }

  @Test
  void extractFileNames_whenFilenameMissing_throwsBadRequest() {
    // Arrange
    CdsData cdsData = prepareCdsDataWithAttachments((Object) null);

    // Act
    ServiceException ex = assertThrows(ServiceException.class, () -> extractFileNames(cdsData));

    // Assert
    assertThat(ex.getErrorStatus()).isEqualTo(ErrorStatuses.BAD_REQUEST);
    assertThat(ex.getMessage()).contains("Filename is missing");
  }

  @Test
  void extractFileNames_whenFilenameNotString_throwsBadRequest() {
    // Arrange
    CdsData cdsData = prepareCdsDataWithAttachments(123);

    // Act
    ServiceException ex = assertThrows(ServiceException.class, () -> extractFileNames(cdsData));

    // Assert
    assertThat(ex.getErrorStatus()).isEqualTo(ErrorStatuses.BAD_REQUEST);
    assertThat(ex.getMessage()).contains("Filename must be a string");
  }

  @Test
  void extractFileNames_multipleFiles_groupedCorrectly() {
    // Arrange
    CdsData cdsData = prepareCdsDataWithAttachments("attachment1.txt", "attachment2.txt");
    mockValidatorExecution("attachment1.txt", "attachment2.txt");

    // Act
    Map<String, Set<String>> result = extractFileNames(cdsData);

    // Assert
    assertThat(result.get(ATTACHMENT_ENTITY))
        .containsExactlyInAnyOrder("attachment1.txt", "attachment2.txt");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("skipConditionsProvider")
  void shouldSkipProcessing_whenConditionNotMet(
      String testName, Consumer<AttachmentDataExtractorTest> setupMock) {

    // Arrange
    setupMock.accept(this);
    CdsData data = prepareCdsDataWithAttachments("file.txt");

    // Act
    Map<String, Set<String>> result = extractFileNames(data);

    // Assert
    assertThat(result).isNotNull();
  }

  private static Stream<Arguments> skipConditionsProvider() {
    return Stream.of(
        Arguments.of(
            "Not an association",
            (Consumer<AttachmentDataExtractorTest>)
                test -> when(test.cdsType.isAssociation()).thenReturn(false)),
        Arguments.of(
            "Not a composition",
            (Consumer<AttachmentDataExtractorTest>)
                test -> when(test.associationType.isComposition()).thenReturn(false)),
        Arguments.of(
            "Not a media entity",
            (Consumer<AttachmentDataExtractorTest>)
                test ->
                    test.helperMock
                        .when(() -> ApplicationHandlerHelper.isMediaEntity(test.targetEntity))
                        .thenReturn(false)),
        Arguments.of(
            "Missing media annotation",
            (Consumer<AttachmentDataExtractorTest>)
                test ->
                    test.mediaMock
                        .when(
                            () ->
                                MediaTypeResolver.getAcceptableMediaTypesAnnotation(
                                    test.targetEntity))
                        .thenReturn(Optional.empty())));
  }

  @Test
  void filter_acceptsElement_whenAllConditionsTrue() {
    CdsData data = prepareCdsDataWithAttachments("file.txt");
    Map<String, Set<String>> result = extractFileNames(data);
    assertThat(result.get(ATTACHMENT_ENTITY)).contains("file.txt");
  }

  @Test
  void ensureFilenamesPresent_whenResultMissingKey_throwsException() {
    // Arrange
    doAnswer(invocation -> processor).when(processor).addValidator(any(), any());
    doNothing().when(processor).process(anyList(), any());
    when(targetEntity.elements()).thenReturn(Stream.of(attachmentElement));
    when(attachmentElement.getName()).thenReturn(ATTACHMENT_FIELD);
    CdsData data =
        CdsData.create(
            Map.of(ATTACHMENT_FIELD, List.of(CdsData.create(Map.of(FILE_NAME, "file.txt")))));

    // Act + Assert
    ServiceException ex = assertThrows(ServiceException.class, () -> extractFileNames(data));
    assertThat(ex.getMessage()).contains("Filename is missing");
  }

  @Test
  void hasMissingFileNames_whenFileNamesEmpty_returnsTrue() throws Exception {
    // Arrange
    Map<String, Set<String>> result = new HashMap<>();
    result.put(ATTACHMENT_ENTITY, new HashSet<>());
    when(attachmentElement.getName()).thenReturn(ATTACHMENT_FIELD);
    when(attachmentElement.getType()).thenReturn(cdsType);
    when(cdsType.as(CdsAssociationType.class)).thenReturn(associationType);
    when(associationType.getTarget()).thenReturn(targetEntity);
    when(targetEntity.getQualifiedName()).thenReturn(ATTACHMENT_ENTITY);
    List<CdsElement> elements = List.of(attachmentElement);
    Set<String> dataKeys = Set.of(ATTACHMENT_FIELD);

    // Act
    var method =
        AttachmentDataExtractor.class.getDeclaredMethod(
            "hasMissingFileNames", Map.class, List.class, Set.class);
    method.setAccessible(true);
    boolean resultValue = (boolean) method.invoke(null, result, elements, dataKeys);

    // Assert
    assertThat(resultValue).isTrue();
  }

  @Test
  void isEmptyValue_shouldCoverAllBranches() throws Exception {
    var method = AttachmentDataExtractor.class.getDeclaredMethod("isEmptyValue", Object.class);
    method.setAccessible(true);
    Iterable<Object> emptyIterable = () -> Collections.emptyIterator();
    Iterable<Object> nonEmptyIterable = () -> List.<Object>of("x").iterator();

    assertThat(method.invoke(null, (Object) null)).isEqualTo(true);
    assertThat(method.invoke(null, "   ")).isEqualTo(true);
    assertThat(method.invoke(null, "abc")).isEqualTo(false);
    assertThat(method.invoke(null, List.of())).isEqualTo(true);
    assertThat(method.invoke(null, List.of("x"))).isEqualTo(false);
    assertThat(method.invoke(null, emptyIterable)).isEqualTo(true);
    assertThat(method.invoke(null, nonEmptyIterable)).isEqualTo(false);
  }

  // ------------------ Mocks and Test Setup ------------------

  private void mockAttachmentElementBasics() {
    when(attachmentElement.getType()).thenReturn(cdsType);
    when(attachmentElement.getDeclaringType()).thenReturn(targetEntity);
    when(attachmentElement.getName()).thenReturn(FILE_NAME);
    when(attachmentElement.getDeclaringType().getQualifiedName()).thenReturn(ATTACHMENT_ENTITY);
  }

  private void mockCdsTypeAsAssociation() {
    when(cdsType.isAssociation()).thenReturn(true);
    when(cdsType.as(CdsAssociationType.class)).thenReturn(associationType);
  }

  private void mockAssociationAsComposition() {
    when(associationType.isComposition()).thenReturn(true);
    when(associationType.getTarget()).thenReturn(targetEntity);
  }

  private void mockTargetEntityDefaults() {
    when(targetEntity.elements()).thenAnswer(inv -> Stream.of(attachmentElement));
    when(targetEntity.getQualifiedName()).thenReturn(ATTACHMENT_ENTITY);
    when(targetEntity.getAnnotationValue(anyString(), any())).thenReturn(Boolean.TRUE);
  }

  private void initStaticMocks() {
    mediaMock = mockStatic(MediaTypeResolver.class);
    helperMock = mockStatic(ApplicationHandlerHelper.class);
    processorMock = mockStatic(CdsDataProcessor.class);
  }

  private void mockDefaultBehavior() {
    mediaMock
        .when(() -> MediaTypeResolver.getAcceptableMediaTypesAnnotation(targetEntity))
        .thenReturn(Optional.of("dummy"));
    helperMock.when(() -> ApplicationHandlerHelper.isMediaEntity(targetEntity)).thenReturn(true);
    processorMock.when(CdsDataProcessor::create).thenReturn(processor);
  }

  private void setupAttachmentModel() {
    mockDefaultBehavior();
    mockAttachmentElementBasics();
    mockCdsTypeAsAssociation();
    mockAssociationAsComposition();
    mockTargetEntityDefaults();
  }

  private Map<String, Set<String>> extractFileNames(CdsData cdsData) {
    return AttachmentDataExtractor.extractFileNamesByElement(targetEntity, List.of(cdsData));
  }

  private CdsData prepareCdsDataWithAttachments(Object... fileNames) {
    List<CdsData> attachments =
        Arrays.stream(fileNames)
            .map(
                name -> {
                  Map<String, Object> map = new HashMap<>();
                  map.put(FILE_NAME, name);
                  return CdsData.create(map);
                })
            .toList();
    mockValidatorExecution(fileNames);
    return CdsData.create(Map.of(ATTACHMENT_FIELD, attachments));
  }

  private void mockValidatorExecution(Object... values) {
    doAnswer(
            invocation -> {
              Filter filter = invocation.getArgument(0);
              Validator validator = invocation.getArgument(1);

              if (filter.test(null, attachmentElement, null)) {
                for (Object value : values) {
                  validator.validate(null, attachmentElement, value);
                }
              }
              return processor;
            })
        .when(processor)
        .addValidator(any(), any());

    doNothing().when(processor).process(anyList(), any());
  }
}
