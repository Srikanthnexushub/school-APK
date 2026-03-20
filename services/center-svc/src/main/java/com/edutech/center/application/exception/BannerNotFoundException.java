// src/main/java/com/edutech/center/application/exception/BannerNotFoundException.java
package com.edutech.center.application.exception;

import java.util.UUID;

public class BannerNotFoundException extends CenterException {
    public BannerNotFoundException(UUID id) {
        super("Banner not found: " + id);
    }
}
