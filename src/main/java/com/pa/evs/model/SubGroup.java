package com.pa.evs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "sub_group")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class SubGroup extends BaseEntity {

	@Column(name = "owner", nullable = false)
	private String owner;// email 

	@JoinColumn(name = "parent_group_name")
	private String parentGroupName; // Group
	
	@Column(name = "name", nullable = false, unique = true)
	private String name;

	@Column(name = "g_desc")
    private String desc;
	
	@Transient
	@Builder.Default
	List<String> roles = new ArrayList<>();
}
