package com.application.devhub.session;

import com.application.devhub.common.crypto.Sha256;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHasher {

    public String hash(String rawToken) {
        return Sha256.hex(rawToken);
    }
}
