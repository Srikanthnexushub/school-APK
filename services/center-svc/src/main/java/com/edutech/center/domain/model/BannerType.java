// src/main/java/com/edutech/center/domain/model/BannerType.java
package com.edutech.center.domain.model;

/**
 * Distinguishes how a banner is rendered on the portal.
 *
 * <ul>
 *   <li>{@link #HERO}   — large hero carousel (AdvertisementBanner component)</li>
 *   <li>{@link #TICKER} — continuous scrolling marquee strip (TickerBanner component)
 *                         intended for third-party advertisers such as event organizers,
 *                         catering companies, uniform suppliers, etc.</li>
 * </ul>
 */
public enum BannerType {
    HERO,
    TICKER
}
