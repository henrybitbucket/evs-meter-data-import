package com.pa.evs.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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
@Getter
@Setter
@Builder
@Entity
@Table(
		name = "dms_project_site",
		indexes = {
				@Index(name = "idx_project_id_site_id_dms_project_site", columnList="project_id,site_id", unique = true)
				
		}
)
public class DMSProjectSite extends BaseEntity {

    @ManyToOne
	@JoinColumn(name = "project_id")
	private DMSProject project;
	
	@ManyToOne
	@JoinColumn(name = "site_id")
	private DMSSite site;
}
