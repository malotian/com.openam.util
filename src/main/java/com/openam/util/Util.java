package com.openam.util;

import java.io.File;
import java.io.IOException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
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

	public static void write(final File file, final Object obj) throws IOException {
		try {
			Util.mapper.writeValue(file, obj);
		} catch (final JsonProcessingException e) {
			e.printStackTrace();
		}
	}
}
