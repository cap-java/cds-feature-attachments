package com.sap.cds.feature.attachments.integrationtests.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.ObjectUtils;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;

@Component
public class MockHttpRequestHelper {

	public static final String ODATA_BASE_URL = "/odata/v4/";
	public static final String FILTER_OPTION = "$filter";

	@Autowired
	private JsonToCapMapperTestHelper mapper;
	@Autowired
	private BatchResponseBodyParser bodyParser;
	@Autowired
	private MockMvc mvc;

	private String contentType = MediaType.APPLICATION_JSON.toString();
	private String accept = MediaType.APPLICATION_JSON.toString();
	private String language = null;

	public MvcResult executeGet(String url) throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(url).contentType(contentType)
																																																			.accept(accept);
		if (!ObjectUtils.isEmpty(language)) {
			requestBuilder.header("Accept-Language", language);
		}
		return mvc.perform(requestBuilder).andReturn();
	}

	public MvcResult executePost(String url, String body) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.post(url).contentType(contentType).accept(accept).content(body))
											.andReturn();
	}

	public void executePostWithMatcher(String url, String body, ResultMatcher matcher) throws Exception {
		mvc.perform(MockMvcRequestBuilders.post(url).contentType(contentType).accept(accept).content(body))
				.andExpect(matcher);
	}

	public MvcResult executeDelete(String url) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.delete(url).contentType(contentType).accept(accept)).andReturn();
	}

	public void executeDeleteWithMatcher(String url, ResultMatcher matcher) throws Exception {
		mvc.perform(MockMvcRequestBuilders.delete(url).contentType(contentType).accept(accept)).andExpect(matcher);
	}

	//	public List<CdsData> executePostWithODataResponseAndAssertStatusCreated(String url, String body) throws Exception {
	//		return executePostWithODataResponseAndAssertStatus(url, body, HttpStatus.CREATED);
	//	}

	//	public List<CdsData> executeBatchWithSingleChangesetAndODataResponseAndAssertStatus(String url,	String body, HttpStatus status) throws Exception {
	//
	//		MvcResult result = executePost(url, body);
	//		assertThat(result.getResponse().getStatus()).isEqualTo(status.value());
	//
	//		String resultBody = result.getResponse().getContentAsString();
	//
	//		String regex = "\\{\"d\":\\{\".*}}}}";
	//		Pattern pattern = Pattern.compile(regex); // the pattern to search for
	//		Matcher matcher = pattern.matcher(resultBody);
	//
	//		if (matcher.find()) {
	//			// we're only looking for one group, so get it
	//			String theGroup = matcher.group(0);
	//			return mapper.mapJsonToResultList(CdsData.class, theGroup);
	//		}
	//
	//		return new ArrayList<>();
	//	}

	//	public List<CdsData> executePostWithODataResponseAndAssertStatus(String url, String body,	HttpStatus status) throws Exception {
	//		MvcResult result = executePost(url, body);
	//		String resultBody = result.getResponse().getContentAsString();
	//		assertThat(result.getResponse().getStatus()).as("Unexpected HTTP status, with response body " + resultBody)
	//				.isEqualTo(status.value());
	//		return mapper.mapResponseToSingleResult(resultBody);
	//	}

	public void executePutWithMatcher(String url, byte[] body, ResultMatcher matcher) throws Exception {
		mvc.perform(MockMvcRequestBuilders.put(url).contentType(contentType).accept(accept).content(body)).andExpect(matcher);
	}

	//	public <T extends CdsData> List<T> executeGetWithODataResponseAndAssertStatus(String url, Class<T> resultType)	throws Exception {
	//		return executeGetWithODataResponseAndAssertStatus(url, resultType, HttpStatus.OK);
	//	}

	public String executeGetWithSingleODataResponseAndAssertStatus(String url, HttpStatus status) throws Exception {
		var result = executeGet(url);
		assertThat(result.getResponse().getStatus()).isEqualTo(status.value());
		return result.getResponse().getContentAsString();
	}

	public <T extends CdsData> T executeGetWithSingleODataResponseAndAssertStatus(String url, Class<T> resultType, HttpStatus status) throws Exception {
		var resultBody = executeGetWithSingleODataResponseAndAssertStatus(url, status);
		return Struct.access(mapper.mapResponseToSingleResult(resultBody)).as(resultType);
	}

	public <T extends CdsData> T getFirstBatchResult(MockHttpServletResponse response, Class<T> resultType) throws Exception {

		List<T> responseData = bodyParser.getBatchResponseData(response, resultType);
		Optional<T> data = responseData.stream().findFirst();
		assertThat(data).as("has result data").isPresent();
		return data.orElseThrow();
	}

	public void assertResponseStatus(MockHttpServletResponse response, HttpStatus status) {
		assertThat(response.getStatus()).as("response status").isEqualTo(status.value());
	}

	public void assertFirstBatchResponseStatus(MockHttpServletResponse response, HttpStatus expectedStatus) throws UnsupportedEncodingException {

		List<HttpStatus> responseStatuses = bodyParser.getBatchResponseStatuses(response);
		Optional<HttpStatus> responseStatus = responseStatuses.stream().findFirst();
		assertThat(responseStatus).as("has response status").isPresent();
		assertThat(responseStatus).as("response status of first request").contains(expectedStatus);
	}

	public void assertSecondBatchResponseStatus(MockHttpServletResponse response, HttpStatus expectedStatus) throws UnsupportedEncodingException {
		List<HttpStatus> responseStatuses = bodyParser.getBatchResponseStatuses(response);
		assertThat(responseStatuses).hasSizeGreaterThanOrEqualTo(2);
		HttpStatus responseStatus = responseStatuses.get(1);
		assertThat(responseStatus).as("response status of first request").isEqualTo(expectedStatus);
	}

	public void assertThirdBatchResponseStatus(MockHttpServletResponse response, HttpStatus expectedStatus) throws UnsupportedEncodingException {
		List<HttpStatus> responseStatuses = bodyParser.getBatchResponseStatuses(response);
		assertThat(responseStatuses).hasSizeGreaterThanOrEqualTo(3);
		HttpStatus responseStatus = responseStatuses.get(2);
		assertThat(responseStatus).as("response status of first request").isEqualTo(expectedStatus);
	}

	public MockHttpRequestHelper setContentType(MediaType contentType) {
		this.contentType = contentType.toString();
		return this;
	}

	public MockHttpRequestHelper setContentType(String contentType) {
		this.contentType = contentType;
		return this;
	}

	public MockHttpRequestHelper setAccept(MediaType accept) {
		this.accept = accept.toString();
		return this;
	}

	public MockHttpRequestHelper setAccept(String accept) {
		this.accept = accept;
		return this;
	}

	public MockHttpRequestHelper setLanguage(String language) {
		this.language = language;
		return this;
	}

	public void resetHelper() {
		contentType = MediaType.APPLICATION_JSON.toString();
		accept = MediaType.APPLICATION_JSON.toString();
		language = null;
	}

}
