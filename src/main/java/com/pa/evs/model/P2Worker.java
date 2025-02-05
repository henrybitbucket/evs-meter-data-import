package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
