package com.edutech.auth.domain.port.in;

import com.edutech.auth.application.dto.DeviceFingerprint;
import com.edutech.auth.application.dto.MfaSetupResponse;
import com.edutech.auth.application.dto.TokenPair;

import java.util.UUID;

public interface MfaUseCase {
    /** Generates a new TOTP secret + QR code URI. Does NOT yet enable MFA. */
    MfaSetupResponse initSetup(UUID userId);

    /** Confirms MFA setup by verifying the first TOTP code. Enables MFA on the account. */
    void confirmSetup(UUID userId, String totpCode);

    /** Disables MFA for the account. Requires a valid TOTP code to prove possession. */
    void disable(UUID userId, String totpCode);

    /** Verifies TOTP code during login and returns the full token pair. */
    TokenPair verifyLogin(String pendingMfaToken, String totpCode, DeviceFingerprint deviceFingerprint);
}
