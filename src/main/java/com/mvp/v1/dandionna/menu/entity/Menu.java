package com.mvp.v1.dandionna.menu.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.mvp.v1.dandionna.common.entity.BaseEntity;
import com.mvp.v1.dandionna.store.entity.ImageStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

/**
 * @author rua
 */

@Entity
@Table(name = "menus")
@Getter
public class Menu extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "store_id", nullable = false)
	private UUID storeId;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "price_krw", nullable = false)
	private int priceKrw;

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

	protected Menu() {}

	private Menu(
		UUID storeId,
		String name,
		String description,
		int priceKrw,
		String imageKey,
		String imageMime,
		String imageEtag,
		ImageStatus imageStatus
	) {
		this.storeId = storeId;
		this.name = name;
		this.description = description;
		this.priceKrw = priceKrw;
		this.imageKey = imageKey;
		this.imageMime = imageMime;
		this.imageEtag = imageEtag;
		if (imageStatus != null) {
			this.imageStatus = imageStatus;
		}
	}

	public static Menu create(UUID storeId, String name, String description, int priceKrw,
		String imageKey, String imageMime, String imageEtag, ImageStatus imageStatus) {
		return new Menu(storeId, name, description, priceKrw, imageKey, imageMime, imageEtag, imageStatus);
	}

	public void update(String name, String description, Integer priceKrw) {
		if (name != null) this.name = name;
		if (description != null) this.description = description;
		if (priceKrw != null) this.priceKrw = priceKrw;
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
