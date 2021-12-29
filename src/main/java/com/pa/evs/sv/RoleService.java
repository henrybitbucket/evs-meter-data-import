package com.pa.evs.sv;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.PermissionDto;
import com.pa.evs.dto.RoleDto;
import com.pa.evs.exception.ApiException;


public interface RoleService {

    void getRoles(PaginDto<RoleDto> pagin);

    void createRole(RoleDto dto) throws ApiException ;

    void updateRole(RoleDto dto);

    void deleteRole(Long id);
    
    void getPermissions(PaginDto<PermissionDto> pagin);
}
