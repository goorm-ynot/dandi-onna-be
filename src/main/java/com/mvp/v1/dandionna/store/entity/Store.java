package com.mvp.v1.dandionna.store.entity;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import com.mvp.v1.dandionna.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "stores")
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

	@Column(nullable = false)
	private String phone;

    @Column(name = "address_road", nullable = false)
    private String addressRoad;

    @Column(nullable = false)
    private BigDecimal lat;

    @Column(nullable = false)
    private BigDecimal lon;

    @Column(columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point geom;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "image_mime")
    private String imageMime;

    @Column(name = "image_etag")
    private String imageEtag;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "image_status", columnDefinition = "menu_image_status", nullable = false)
    private ImageStatus imageStatus = ImageStatus.pending;

    protected Store() {
    }

	private Store(UUID ownerUserId, String name, String category, String phone, String addressRoad,
		BigDecimal lat, BigDecimal lon, Point geom, LocalTime openTime, LocalTime closeTime,
		String description, String imageKey, String imageMime, String imageEtag, ImageStatus imageStatus) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.category = category;
        this.phone = phone;
        this.addressRoad = addressRoad;
        this.lat = lat;
        this.lon = lon;
        this.geom = geom;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.description = description;
        this.imageKey = imageKey;
        this.imageMime = imageMime;
        this.imageEtag = imageEtag;
        if (imageStatus != null) {
            this.imageStatus = imageStatus;
        }
    }

	public static Store create(UUID ownerUserId, String name, String category, String phone, String addressRoad,
        BigDecimal lat, BigDecimal lon, Point geom, LocalTime openTime, LocalTime closeTime, String description,
        String imageKey, String imageMime, String imageEtag, ImageStatus imageStatus) {
        return new Store(ownerUserId, name, category, phone, addressRoad, lat, lon, geom, openTime, closeTime, description,
            imageKey, imageMime, imageEtag, imageStatus);
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

	public String getPhone() {
		return phone;
	}

    public String getAddressRoad() {
        return addressRoad;
    }

    public BigDecimal getLat() {
        return lat;
    }

    public BigDecimal getLon() {
        return lon;
    }

    public Point getGeom() {
        return geom;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public String getDescription() {
        return description;
    }

    public String getImageKey() {
        return imageKey;
    }

    public String getImageMime() {
        return imageMime;
    }

    public String getImageEtag() {
        return imageEtag;
    }

    public ImageStatus getImageStatus() {
        return imageStatus;
    }

	public void update(String name, String category, String phone, String addressRoad,
		BigDecimal lat, BigDecimal lon, Point geom, LocalTime openTime, LocalTime closeTime, String description) {
		this.name = name;
		this.category = category;
		this.phone = phone;
		this.addressRoad = addressRoad;
		this.lat = lat;
		this.lon = lon;
        this.geom = geom;
		this.openTime = openTime;
		this.closeTime = closeTime;
        this.description = description;
	}

	public void updateImage(String key, String mime, String etag, ImageStatus status) {
		this.imageKey = key;
		this.imageMime = mime;
		this.imageEtag = etag;
        if (status != null) {
            this.imageStatus = status;
        }
    }
}
