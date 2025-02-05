package com.pa.evs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "sub_group_member_role")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class SubGroupMemberRole extends BaseEntity {

	@Column(name = "email", nullable = false)
	private String email;
	
	@Column(name = "r_desc")
    private String desc;
    
	@Column(name = "role", nullable = false)
    private String role;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_group_member_id")
    private SubGroupMember member;
}
