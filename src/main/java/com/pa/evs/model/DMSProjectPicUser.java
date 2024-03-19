package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
		name = "dms_project_pic_user",
		indexes = {
				@Index(name = "idx_project_id_pic_user_id_dms_project_pic_user", columnList="project_id,pic_user_id", unique = true)
				
		}
)
public class DMSProjectPicUser extends BaseEntity {

    @ManyToOne
	@JoinColumn(name = "project_id")
	private DMSProject project;
	
	@ManyToOne
	@JoinColumn(name = "pic_user_id")
	private Users picUser;
	
	@Column(name = "is_sub_pic", columnDefinition = "boolean default false")
	private boolean isSubPic;// location key to get location lock

}
