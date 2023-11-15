package com.pa.evs.sv;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.pa.evs.dto.ChangePasswordDto;
import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.LoginRequestDto;
import com.pa.evs.dto.LoginResponseDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.PlatformUserLoginDto;
import com.pa.evs.dto.ResetPasswordDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.model.SubGroup;
import com.pa.evs.security.user.JwtUser;

public interface AuthenticationService {
	
    ResponseDto<LoginResponseDto> login(LoginRequestDto loginRequestDTO);
    ResponseDto<JwtUser>getUser(HttpServletRequest request);
    Object getUsernameById(Long userId);
    Object getUserById(Long userId);
    Object preLogin(String username);
	void save(UserDto dto);
	void saveRole (UserDto dto);
	void saveGroup (UserDto dto);
	void savePermission (UserDto dto);
	
	void initDataAuths();
	void getUsers(PaginDto<UserDto> pagin);
	void getPermissionsOfUser(PaginDto<UserDto> pagin);
	void getRoleOfUserLogin(PaginDto<RoleDto> pagin);
	void getRoleOfUser(PaginDto<RoleDto> pagin);
	void getGroupOfUser(PaginDto<GroupUserDto> pagin);
	void getPermissionsEachUser(PaginDto<PermissionDto> pagin);
	void removeUserById(Long userId);
	List<PlatformUserLoginDto> getPfOfUser(String email);
	void savePfOfUser(PlatformUserLoginDto dto);
	ResponseDto<? extends Object> sendOtp(Map<String, Object> dto);
	void changePwd(ChangePasswordDto changePasswordDto);
	void resetPwd(ResetPasswordDto resetPasswordDto);
	void updatePhoneNumber(String phoneNumber);
	boolean validatePassword(LoginRequestDto loginRequestDTO);
	void saveSubGroup(Map<String, Object> payload);
	void addUserToSubGroup(Map<String, Object> payload);
	void removeUserFromSubGroup(Map<String, Object> payload);
	void addRoleToSubGroupMember(Map<String, Object> payload);
	void removeRoleFromSubGroupMember(Map<String, Object> payload);
	List<SubGroup> getSubGroupOfUser(String email);
	
	List<SubGroup> getSubGroupOwner();
	void deleteSubGroup(Long id);
	@SuppressWarnings("rawtypes")
	List getUserOfSubGroup(Map<String, Object> payload);
	void getRoleOfMemberSubGroup(PaginDto<RoleDto> pagin);
}
