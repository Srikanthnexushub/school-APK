// src/main/java/com/edutech/center/domain/model/BannerType.java
package com.edutech.center.domain.model;

/**
 * Distinguishes how a banner is rendered on the portal.
 *
 * <ul>
 *   <li>{@link #HERO}         — large hero carousel (AdvertisementBanner component)</li>
 *   <li>{@link #TICKER}       — continuous scrolling marquee strip (TickerBanner component)</li>
 *   <li>{@link #VIDEO}        — full-bleed autoplay video advertisement (VideoBanner component)</li>
 *   <li>{@link #FOOTER_VIDEO} — compact autoplay video strip in the footer area (FooterBanner component)</li>
 * </ul>
 */
public enum BannerType {
    HERO,
    TICKER,
    VIDEO,
    FOOTER_VIDEO
}
