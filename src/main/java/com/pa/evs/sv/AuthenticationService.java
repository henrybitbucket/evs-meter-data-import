package com.pa.evs.sv;

import javax.servlet.http.HttpServletRequest;

import com.pa.evs.dto.LoginRequestDto;
import com.pa.evs.dto.LoginResponseDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ResponseDto;
import com.pa.evs.dto.UserDto;
import com.pa.evs.security.user.JwtUser;

public interface AuthenticationService {
	
    ResponseDto<LoginResponseDto> login(LoginRequestDto loginRequestDTO);
    ResponseDto<JwtUser>getUser(HttpServletRequest request);
    Object getUsernameById(Long userId);
    Object getUserById(Long userId);
	void save(UserDto dto);
	void saveRole (UserDto dto);
	void saveGroup (UserDto dto);
	
	void initDataAuths();
	void getUsers(PaginDto<UserDto> pagin);
	void removeUserById(Long userId);
}
