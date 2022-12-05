package com.openam.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Util {
	private static ObjectMapper mapper = new ObjectMapper();

	public static String json(final Object obj) {
		try {
			return Util.mapper.writeValueAsString(obj);
		} catch (final JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
