package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserDto {
	private Long id;
	private String username;
    private String email;
    private String fullName;
    private String firstName;
    private String lastName;
    private String gender;
    private String avatar;
    private Date birthDay;
    private String identification;
    private String phoneNumber;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String password;
    
    private Boolean updatePwd;
    
    private Boolean sendLoginToPhone;
    
    private Boolean sendLoginToEmail;
    
    private Long approved;
    
    @Builder.Default
    private List<String> appCodes = new ArrayList<>();
    
    @Builder.Default
    private List<String> roles = new ArrayList<>();
    
    @Builder.Default
    private List<String> projects = new ArrayList<>();
    
    @Builder.Default
    private List<String> companies = new ArrayList<>();
    
    @Builder.Default
    private List<Map<String, Object>> roleDescs = new ArrayList<>();
    
    private String status;
    
    private List<RoleDto> role;
    
    private List<GroupUserDto> groupUsers;
    
    private List<PermissionDto> permissions;

    private Boolean changePwdRequire;
    
    private Boolean loginOtpRequire;
    
}
