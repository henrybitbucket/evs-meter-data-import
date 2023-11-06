package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

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
	name = "p2_worker",
	indexes = {
		@Index(columnList = "email", name = "idx_email_p2_worker"),
		@Index(columnList = "manager", name = "idx_manager_p2_worker"),
		@Index(columnList = "manager,email", name = "idx_manager_email_p2_worker"),
	},
	uniqueConstraints = @UniqueConstraint(columnNames={"email", "manager"})
)
public class P2Worker extends Base1Entity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
    private Long id;
	
	@Column(name = "email")
    private String email;
	
	@Column(name = "manager")
    private String manager;
	
	@Column(name = "updated_by")
    private String updatedBy;
}
