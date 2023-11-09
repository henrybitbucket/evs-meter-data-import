package com.pa.evs.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
    @JoinColumn(name = "sub_group_id")
    private SubGroupMember member;
}
