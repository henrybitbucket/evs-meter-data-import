package com.pa.evs.security.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class JwtUser implements UserDetails {

    /**
	 * 
	 */
	private static final long serialVersionUID = -7870496055245376597L;

	private Long id;

    @JsonProperty("username")
    private  String fullName;
    @JsonIgnore
    private  String password;

    @JsonIgnore
    private  String email;

    @JsonProperty("email")
    private String username;
    
    private String phone;

    private String gender;
    
    private String avatar;
    
    private String firstName;
    
    private String lastName;
    
    private List<String> groups;
    
    private Date birthDay;
    
    private Boolean changePwdRequire = false;
    
    private String phoneNumber;

    private Collection<? extends GrantedAuthority> authorities;
    
    private Collection<String> permissions;
    
    @Builder.Default
    private List<String> projects = new ArrayList<>();
    
    @Builder.Default
    private List<String> appCodes = new ArrayList<>();

    @JsonIgnore
    private  boolean enabled;
    private  Date lastPasswordResetDate;
    
    @JsonIgnore
    private  Date tokenExpireDate;

//    public JwtUser(
//          Long id,
//          String username,
//          String email,
//          String password,
//          Collection<? extends GrantedAuthority> authorities,
//          boolean enabled,
//          Date lastPasswordResetDate
//    ) {
//        this.id = id;
//        this.username = username;
//        this.fullname = email;
//        this.password = password;
//        this.authorities = authorities;
//        this.enabled = enabled;
//        this.lastPasswordResetDate = lastPasswordResetDate;
//    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }


    @JsonIgnore
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @JsonIgnore
    public Date getLastPasswordResetDate() {
        return lastPasswordResetDate;
    }


    public Long getId() {
        return id;
    }

	public void setChangePwdRequire(Boolean changePwdRequire) {
		this.changePwdRequire = changePwdRequire;
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}

	public void setPermissions(Collection<String> permissions) {
		this.permissions = permissions;
	}

	public void setProjects(List<String> projects) {
		this.projects = projects;
	}

	public void setTokenExpireDate(Date tokenExpireDate) {
		this.tokenExpireDate = tokenExpireDate;
	}
	
	
}
