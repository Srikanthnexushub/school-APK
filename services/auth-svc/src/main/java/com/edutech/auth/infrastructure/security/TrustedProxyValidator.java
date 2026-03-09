package com.edutech.auth.infrastructure.security;

import com.edutech.auth.infrastructure.config.SecurityProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Validates whether a remote address belongs to a trusted proxy CIDR range.
 * Only trusted proxy IPs are allowed to set X-Forwarded-For headers.
 */
@Component
public class TrustedProxyValidator {

    private final List<String> trustedCidrs;

    public TrustedProxyValidator(SecurityProperties securityProperties) {
        this.trustedCidrs = securityProperties.trustedProxyCidrs();
    }

    public boolean isTrustedProxy(String remoteAddr) {
        if (trustedCidrs == null || trustedCidrs.isEmpty()) return false;
        try {
            InetAddress addr = InetAddress.getByName(remoteAddr);
            for (String cidr : trustedCidrs) {
                if (isInCidr(addr, cidr)) return true;
            }
        } catch (UnknownHostException e) {
            return false;
        }
        return false;
    }

    private boolean isInCidr(InetAddress addr, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress network = InetAddress.getByName(parts[0]);
            int prefixLen = Integer.parseInt(parts[1]);
            byte[] addrBytes = addr.getAddress();
            byte[] networkBytes = network.getAddress();
            if (addrBytes.length != networkBytes.length) return false;
            int fullBytes = prefixLen / 8;
            int remainingBits = prefixLen % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (addrBytes[i] != networkBytes[i]) return false;
            }
            if (remainingBits > 0 && fullBytes < addrBytes.length) {
                int mask = 0xFF & (0xFF << (8 - remainingBits));
                return (addrBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
