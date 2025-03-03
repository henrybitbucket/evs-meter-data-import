package com.pa.evs.sv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface StarfishCAService {

	Map<String, Object> requestCA(InputStream csr, String entityUsername, String certProfileId, String caId) throws IOException;

	String formatCA(String base64) throws Exception;

}
