package com.pa.evs.model;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
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
@Table(name = "users")
public class Users extends Base1Entity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
    private Long userId;
	
	@Column(name = "username")
    private String username;
	
	@Column(name = "email")
    private String email;
	
	@Column(name = "full_name")
    private String fullName;
	
	@Column(name = "first_name")
    private String firstName;
	
	@Column(name = "last_name")
    private String lastName;
	
	@Column(name = "password")
    private String password;
	
	@Column(name = "phone_number")
    private String phoneNumber;
	
	@Column(name = "approved")
    private Long approved;
	
	@Column(name = "status")
    private String status;

	@OneToMany(fetch=FetchType.LAZY, mappedBy = "user", cascade = CascadeType.MERGE)
    private List<UserRole> roles;
    
    @Column(name = "avatar")
    private String avatar;
    
    @Column(name = "token")
    private String token;
    
    @Column(name = "last_login")
    private Date lastLogin;
    
    @Builder.Default
    @Column(name = "change_pwd_require", columnDefinition = "boolean default false not null")
    private Boolean changePwdRequire = false;
    
}
