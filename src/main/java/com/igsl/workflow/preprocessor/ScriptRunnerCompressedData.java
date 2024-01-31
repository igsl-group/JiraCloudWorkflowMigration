package com.igsl.workflow.preprocessor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.http.client.entity.GZIPInputStreamFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ScriptRunnerCompressedData implements ValueProcessor {
	private static final String COMPRESSED = "compressed";
	private static final ObjectMapper OM = new ObjectMapper();
	@Override
	public String unpack(String value) throws ValueProcessorException {
		try {
			if (value != null) {
				// Base64 decode
				byte[] decoded = Base64.getDecoder().decode(value);
				// Convert JSON to byte array
				JsonNode node = OM.readTree(decoded);
				JsonNode data = node.get(COMPRESSED);
				byte[] zipData = null;
				try (	ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					if (data.isArray()) {
						for (JsonNode n : data) {
							baos.write(n.asInt());
						}
					}
					zipData = baos.toByteArray();
				}
				// Deflate GZIP
				try (	ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
						ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
						BufferedInputStream bis = new BufferedInputStream(bais);
						InputStream gis = new GZIPInputStreamFactory().create(bis)) {
					int c = -1;
					while ((c = gis.read()) != -1) {
						baos2.write(c);
					}
					return baos2.toString();
				}
			}
			return null;
		} catch (Exception ex) {
			throw new ValueProcessorException(ex);
		}
	}
	@Override
	public String pack(String value) throws ValueProcessorException {
		try {
			if (value != null && !value.isBlank()) {
				// GZip
				List<Integer> list = new ArrayList<Integer>(); 
				try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
					try (	BufferedOutputStream bos = new BufferedOutputStream(baos); 
							GZIPOutputStream gos = new GZIPOutputStream(bos)) {
						gos.write(value.getBytes());
					}
					byte[] data = baos.toByteArray();
					for (byte b : data) {
						list.add((int) (b & 0xFF));
					}
				}
				// Pack as Json
				Map<String, Object> map = new HashMap<>();
				map.put(COMPRESSED, list);
				String json = OM.writeValueAsString(map);
				// Base64 encode
				return Base64.getEncoder().encodeToString(json.getBytes());
			}
			return null;
		} catch (Exception ex) {
			throw new ValueProcessorException(ex);
		}
	}

}
