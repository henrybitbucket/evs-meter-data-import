package com.pa.evs.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "dms_project")
public class DMSProject extends BaseEntity {

	@Column(name = "name", unique = true)
	private String name;//Contract No. 91204683
	
	@Column(name = "display_name", unique = true)
	private String displayName; //ptwp-240315-35678
	
	@Column(name = "p_start", nullable = false)
	private Long start;
	
	@Column(name = "p_end", nullable = false)
	private Long end;
	
	@OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
	@Builder.Default
	private List<DMSProjectPicUser> picUsers = new ArrayList<>();// PIC -> portal user, 1 proj -> 1 picUser and list sub pic user. PIC -> user in group DMS_G_PIC (role DMS_R_APPROVE_APPLICATION, DMS_R_REJECT_APPLICATION)
}
