package com.edutech.center.domain.port.out;

import java.util.UUID;

/**
 * Port for validating that a parent has an active student link at a given center.
 * Implemented by an adapter that calls parent-svc over HTTP (service-to-service).
 */
public interface ParentLinkValidationPort {

    /**
     * Returns true if the parent (identified by their auth userId) has at least one
     * active student link whose centerId matches the given centerId.
     *
     * <p>Fail-closed: returns false on any error (timeout, 4xx, 5xx, network).
     *
     * @param parentUserId the parent's userId from the JWT sub claim
     * @param centerId     the center to check access against
     * @return true if the parent is linked to the center, false otherwise
     */
    boolean isParentLinkedToCenter(UUID parentUserId, UUID centerId);
}
