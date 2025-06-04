package com.sap.cds.feature.attachments.handler.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sap.cds.CdsData;

class ApplicationHandlerHelperTest {

	@Test
	void keysAreInData() {
		Map<String, Object> keys = Map.of("key1", "value1", "key2", "value2");
		var data = CdsData.create();
		data.put("key1", "value1");
		data.put("key2", "value2");
		data.put("data", "value3");
		var result = ApplicationHandlerHelper.areKeysInData(keys, data);

		assertThat(result).isTrue();
	}

	@Test
	void keyMissingInData() {
		Map<String, Object> keys = Map.of("key1", "value1", "key2", "value2");
		var data = CdsData.create();
		data.put("key1", "value1");
		data.put("data", "value3");
		var result = ApplicationHandlerHelper.areKeysInData(keys, data);

		assertThat(result).isFalse();
	}

	@Test
	void keyHasWrongValue() {
		Map<String, Object> keys = Map.of("key1", "value1", "key2", "value2");
		var data = CdsData.create();
		data.put("key1", "value1");
		data.put("key2", "wrong value");
		data.put("data", "value3");
		var result = ApplicationHandlerHelper.areKeysInData(keys, data);

		assertThat(result).isFalse();
	}

	@Test
	void removeDraftKeys() {
		Map<String, Object> keys = Map.of("key1", "value1", "IsActiveEntity", "true");
		assertTrue(keys.containsKey("IsActiveEntity"));

		Map<String, Object> result = ApplicationHandlerHelper.removeDraftKey(keys);
		assertFalse(result.containsKey("IsActiveEntity"));
		assertTrue(result.containsKey("key1"));
	}
}
