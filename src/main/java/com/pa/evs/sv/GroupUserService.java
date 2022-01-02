package com.pa.evs.sv;

import com.pa.evs.dto.GroupUserDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;


public interface GroupUserService {

    void getGroupUser(PaginDto<GroupUserDto> pagin);

    void createGroupUser(GroupUserDto dto) throws ApiException ;

    void updateGroupUser(GroupUserDto dto) throws Exception;

    void deleteGroupUser(Long id);
    
}
