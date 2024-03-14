package com.sap.cds.feature.attachments.integrationtests.common;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;

import com.sap.cds.CdsData;
import com.sap.cds.Struct;

@Component
public class BatchResponseBodyParser {

	private static final String HTTP_PROTOCOL = "HTTP/1.1";
	private static final int HTTP_STATUS_LENGTH = 3;

	private static final String DATA_REGEX = "\\{\"d\":\\{\".*}}}}";

	@Autowired
	private JsonToCapMapperTestHelper mapper;

	public List<HttpStatus> getBatchResponseStatuses(MockHttpServletResponse response)	throws UnsupportedEncodingException {

		String body = response.getContentAsString();
		List<HttpStatus> responseStatuses = new ArrayList<>();

		Stream.of(body.split(HTTP_PROTOCOL)).skip(1).forEach(responsePart -> {
			int responseStatus = Integer.parseInt(responsePart.substring(1, 1 + HTTP_STATUS_LENGTH));
			responseStatuses.add(HttpStatus.valueOf(responseStatus));
		});

		return responseStatuses;
	}

	public <T extends CdsData> List<T> getBatchResponseData(MockHttpServletResponse response, Class<T> resultType)	throws Exception {
		List<T> responseData = new ArrayList<>();

		String body = response.getContentAsString();
		Pattern pattern = Pattern.compile(DATA_REGEX);
		Matcher matcher = pattern.matcher(body);

		if (matcher.find()) {
			String dataAsString = matcher.group(0);
			var data = mapper.mapResponseToSingleResult(dataAsString);
			responseData.add(Struct.access(data).as(resultType));
		}

		return responseData;
	}

}
