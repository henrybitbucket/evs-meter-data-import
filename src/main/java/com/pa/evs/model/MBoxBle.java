package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "m_box_ble")
public class MBoxBle extends BaseEntity {
	@Column(name = "box_uuid", length = 255, unique = true, nullable = false)
    private String boxUuid;

    @Column(name = "home_address", length = 255)
    private String homeAddress;

    @Column(name = "name", length = 255, unique = true)
    private String name;

    @Column(name = "remark", length = 255, unique = true)
    private String remark;
    
    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    @ManyToOne
    @JoinColumn(name = "block_id")
    private Block block;

    @ManyToOne
    @JoinColumn(name = "building_id")
    private Building building;

    @ManyToOne
    @JoinColumn(name = "building_unit_id")
    private BuildingUnit buildingUnit;

    @ManyToOne
    @JoinColumn(name = "floor_level_id")
    private FloorLevel floorLevel;
}
