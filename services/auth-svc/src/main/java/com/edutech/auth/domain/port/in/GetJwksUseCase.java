package com.edutech.auth.domain.port.in;

import java.util.Map;

public interface GetJwksUseCase {
    /** Returns the JWKS document as a map (serializable to JSON). */
    Map<String, Object> getJwks();
}
