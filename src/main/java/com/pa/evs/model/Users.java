package com.pa.evs.model;

import java.util.ArrayList;
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
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

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
	
	@Column(name = "email", unique = true)
    private String email;
	
	@Column(name = "full_name")
    private String fullName;
	
	@Column(name = "first_name")
    private String firstName;
	
	@Column(name = "last_name")
    private String lastName;
	
	@Column(name = "password")
    private String password;
	
	@Column(name = "phone_number", unique = true)
    private String phoneNumber;
	
	@Column(name = "approved")
    private Long approved;
	
	@Column(name = "status")
    private String status;

	@OneToMany(fetch=FetchType.LAZY, mappedBy = "user", cascade = CascadeType.MERGE)
    private List<UserAppCode> appCodes;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy = "user", cascade = CascadeType.MERGE)
    private List<UserRole> roles;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy = "user", cascade = CascadeType.MERGE)
    private List<UserPermission> permissions;
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy = "user", cascade = CascadeType.MERGE)
    private List<UserProject> projects;
    
    @Column(name = "avatar")
    private String avatar;
    
    @Column(name = "token")
    private String token;
    
    @Column(name = "last_pwd", length = 1000, nullable = true)
    private String lastPwd;

    @Column(name = "last_login")
    private Date lastLogin;
    
    @Builder.Default
    @Column(name = "change_pwd_require", columnDefinition = "boolean default false not null")
    private Boolean changePwdRequire = false;

    @Column(name = "last_change_pwd")
    private Long lastChangePwd;
    
    @Builder.Default
    @Column(name = "login_otp_require", columnDefinition = "boolean default false not null")
    private Boolean loginOtpRequire = false;
    
    private String identification;

    @Transient
    @Builder.Default
    private List<SubGroup> subGroups = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<String> allAppCodes = new ArrayList<>();
    
    @Transient
    @Builder.Default
    private List<String> allPermissions = new ArrayList<>();
    
    @Transient
    @Builder.Default
    private List<String> allRoles = new ArrayList<>();
    
    @Transient
    @Builder.Default
    private List<String> allProjects = new ArrayList<>();
    
    public String getLastPwd() {
    	if (StringUtils.isBlank(lastPwd)) {
			lastPwd = "";
		}
		if (StringUtils.isBlank(lastPwd) && StringUtils.isNotBlank(this.password)) {
			lastPwd = "[" + password + "]" + lastPwd;
		}
    	return lastPwd;
    }

    public void setPassword(String password) {
    	try {
			if (StringUtils.isBlank(lastPwd)) {
				lastPwd = "[" + this.password + "]";
			}
			if (StringUtils.isNotBlank(password)) {
				lastPwd = "[" + password + "]" + lastPwd;
			}
			lastPwd = lastPwd.replaceAll("^((?:(\\[[^\\[\\]]+\\])){3}).*$", "$1");
		} catch (Exception e) {
			//
		}
    	this.password = password;
    }

}
