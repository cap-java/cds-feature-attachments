package com.sap.cds.feature.attachments.integrationtests.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;

@Component
public class MockHttpRequestHelper {

	public static final String ODATA_BASE_URL = "/odata/v4/";

	@Autowired
	private JsonToCapMapperTestHelper mapper;
	@Autowired
	private MockMvc mvc;

	private String contentType = MediaType.APPLICATION_JSON.toString();
	private String accept = MediaType.APPLICATION_JSON.toString();

	public MvcResult executeGet(String url) throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(url).contentType(contentType).accept(
				accept);
		return mvc.perform(requestBuilder).andReturn();
	}

	public String executeGetWithSingleODataResponseAndAssertStatus(String url, HttpStatus status) throws Exception {
		var result = executeGet(url);
		assertThat(result.getResponse().getStatus()).isEqualTo(status.value());
		return result.getResponse().getContentAsString();
	}

	public <T extends CdsData> T executeGetWithSingleODataResponseAndAssertStatus(String url, Class<T> resultType,
																																																																															HttpStatus status) throws Exception {
		var resultBody = executeGetWithSingleODataResponseAndAssertStatus(url, status);
		return Struct.access(mapper.mapResponseToSingleResult(resultBody)).as(resultType);
	}

	public MvcResult executePost(String url, String body) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.post(url).contentType(contentType).accept(accept).content(body))
											.andReturn();
	}

	public MvcResult executePatch(String url, String body) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.patch(url).contentType(contentType).accept(accept).content(body))
											.andReturn();
	}

	public void executePostWithMatcher(String url, String body, ResultMatcher matcher) throws Exception {
		mvc.perform(MockMvcRequestBuilders.post(url).contentType(contentType).accept(accept).content(body)).andExpect(
				matcher);
	}

	public MvcResult executeDelete(String url) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.delete(url).contentType(contentType).accept(accept)).andReturn();
	}

	public void executeDeleteWithMatcher(String url, ResultMatcher matcher) throws Exception {
		mvc.perform(MockMvcRequestBuilders.delete(url).contentType(contentType).accept(accept)).andExpect(matcher);
	}

	public CdsData executePostWithODataResponseAndAssertStatusCreated(String url, String body) throws Exception {
		return executePostWithODataResponseAndAssertStatus(url, body, HttpStatus.CREATED);
	}

	public void executePatchWithODataResponseAndAssertStatusOk(String url, String body) throws Exception {
		executePatchWithODataResponseAndAssertStatus(url, body, HttpStatus.OK);
	}

	public CdsData executePostWithODataResponseAndAssertStatus(String url, String body,
																																																												HttpStatus status) throws Exception {
		MvcResult result = executePost(url, body);
		String resultBody = result.getResponse().getContentAsString();
		assertThat(result.getResponse().getStatus()).as("Unexpected HTTP status, with response body " + resultBody).isEqualTo(
				status.value());
		return mapper.mapResponseToSingleResult(resultBody);
	}

	public void executePatchWithODataResponseAndAssertStatus(String url, String body, HttpStatus status) throws Exception {
		MvcResult result = executePatch(url, body);
		String resultBody = result.getResponse().getContentAsString();
		assertThat(result.getResponse().getStatus()).as("Unexpected HTTP status, with response body " + resultBody).isEqualTo(
				status.value());
	}

	public void executePutWithMatcher(String url, byte[] body, ResultMatcher matcher) throws Exception {
		mvc.perform(MockMvcRequestBuilders.put(url).contentType(contentType).accept(accept).content(body)).andExpect(matcher);
	}

	public void setContentType(MediaType contentType) {
		this.contentType = contentType.toString();
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void resetHelper() {
		contentType = MediaType.APPLICATION_JSON.toString();
		accept = MediaType.APPLICATION_JSON.toString();
	}

}
