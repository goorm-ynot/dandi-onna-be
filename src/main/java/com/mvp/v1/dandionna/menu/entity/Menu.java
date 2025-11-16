package com.mvp.v1.dandionna.menu.entity;

import java.util.UUID;

import com.mvp.v1.dandionna.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

@Entity
@Table(name = "menus")
@Getter
public class Menu extends BaseEntity {

    @Id
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_krw", nullable = false)
    private int priceKrw;

}
