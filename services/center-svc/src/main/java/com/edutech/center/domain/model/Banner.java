// src/main/java/com/edutech/center/domain/model/Banner.java
package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Platform-level promotional banner managed by SUPER_ADMIN.
 * Displayed on Parent and Institution (CENTER_ADMIN) dashboards.
 *
 * <p>State is mutated through named domain methods — no arbitrary setters exposed
 * beyond what the application service requires for construction.
 * Soft-deletion via {@code deletedAt}; hard-deletes are never performed.
 */
@Entity
@Table(name = "banners", schema = "center_schema")
public class Banner {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String subtitle;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "video_url", columnDefinition = "TEXT")
    private String videoUrl;

    @Column(name = "link_url", columnDefinition = "TEXT")
    private String linkUrl;

    @Column(name = "link_label", columnDefinition = "TEXT")
    private String linkLabel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BannerAudience audience;

    @Enumerated(EnumType.STRING)
    @Column(name = "banner_type", nullable = false, length = 10)
    private BannerType bannerType;

    @Column(name = "bg_color", columnDefinition = "TEXT")
    private String bgColor;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Version
    private Long version;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Required by JPA. */
    protected Banner() {}

    /**
     * Factory method — creates a new banner with the given properties.
     * The banner is created active by default.
     */
    public static Banner create(String title, String subtitle, String imageUrl, String videoUrl,
                                String linkUrl, String linkLabel, BannerAudience audience,
                                String bgColor, int displayOrder, Instant startDate, Instant endDate,
                                BannerType bannerType) {
        Banner b = new Banner();
        b.title        = title;
        b.subtitle     = subtitle;
        b.imageUrl     = imageUrl;
        b.videoUrl     = videoUrl;
        b.linkUrl      = linkUrl;
        b.linkLabel    = linkLabel;
        b.audience     = audience;
        b.bgColor      = bgColor;
        b.displayOrder = displayOrder;
        b.isActive     = true;
        b.startDate    = startDate;
        b.endDate      = endDate;
        b.bannerType   = bannerType != null ? bannerType : BannerType.HERO;
        b.createdAt    = Instant.now();
        b.updatedAt    = Instant.now();
        return b;
    }

    // ─── Domain state transitions ──────────────────────────────────────────────

    /** Makes this banner visible to users. */
    public void activate() {
        this.isActive = true;
        this.updatedAt = Instant.now();
    }

    /** Hides this banner from users without deleting it. */
    public void deactivate() {
        this.isActive = false;
        this.updatedAt = Instant.now();
    }

    /** Soft-deletes the banner. Excluded from all queries after this point. */
    public void softDelete() {
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Updates editable banner details.
     * Only non-null parameters are applied (PATCH semantics).
     */
    public void updateDetails(String title, String subtitle, String imageUrl, String videoUrl,
                              String linkUrl, String linkLabel, String bgColor,
                              int displayOrder, Instant startDate, Instant endDate) {
        if (title     != null) this.title     = title;
        if (subtitle  != null) this.subtitle  = subtitle;
        if (imageUrl  != null) this.imageUrl  = imageUrl;
        if (videoUrl  != null) this.videoUrl  = videoUrl;
        if (linkUrl   != null) this.linkUrl   = linkUrl;
        if (linkLabel != null) this.linkLabel = linkLabel;
        if (bgColor   != null) this.bgColor   = bgColor;
        this.displayOrder = displayOrder;
        if (startDate != null) this.startDate = startDate;
        if (endDate   != null) this.endDate   = endDate;
        this.updatedAt = Instant.now();
    }

    /**
     * Returns true when this banner should currently be shown to users:
     * active, not deleted, and within its optional date window.
     */
    public boolean isCurrentlyActive() {
        Instant now = Instant.now();
        return isActive
                && deletedAt == null
                && (startDate == null || !now.isBefore(startDate))
                && (endDate   == null || !now.isAfter(endDate));
    }

    // ─── Setters (used by service during construction) ─────────────────────────

    public void setTitle(String title)               { this.title = title; }
    public void setSubtitle(String subtitle)         { this.subtitle = subtitle; }
    public void setImageUrl(String imageUrl)         { this.imageUrl = imageUrl; }
    public void setVideoUrl(String videoUrl)         { this.videoUrl = videoUrl; }
    public void setLinkUrl(String linkUrl)           { this.linkUrl = linkUrl; }
    public void setLinkLabel(String linkLabel)       { this.linkLabel = linkLabel; }
    public void setAudience(BannerAudience audience) { this.audience = audience; }
    public void setBannerType(BannerType bannerType)  { this.bannerType = bannerType; }
    public void setBgColor(String bgColor)           { this.bgColor = bgColor; }
    public void setDisplayOrder(int displayOrder)    { this.displayOrder = displayOrder; }
    public void setIsActive(boolean isActive)        { this.isActive = isActive; }
    public void setStartDate(Instant startDate)      { this.startDate = startDate; }
    public void setEndDate(Instant endDate)          { this.endDate = endDate; }
    public void setCreatedAt(Instant createdAt)      { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt)      { this.updatedAt = updatedAt; }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public UUID getId()              { return id; }
    public String getTitle()         { return title; }
    public String getSubtitle()      { return subtitle; }
    public String getImageUrl()      { return imageUrl; }
    public String getVideoUrl()      { return videoUrl; }
    public String getLinkUrl()       { return linkUrl; }
    public String getLinkLabel()     { return linkLabel; }
    public BannerAudience getAudience() { return audience; }
    public BannerType getBannerType()   { return bannerType; }
    public String getBgColor()       { return bgColor; }
    public int getDisplayOrder()     { return displayOrder; }
    public boolean isActive()        { return isActive; }
    public Instant getStartDate()    { return startDate; }
    public Instant getEndDate()      { return endDate; }
    public Long getVersion()         { return version; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }
    public Instant getDeletedAt()    { return deletedAt; }
}
