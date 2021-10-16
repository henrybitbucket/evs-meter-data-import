package com.pa.evs.sv;

import java.io.IOException;

import org.springframework.transaction.annotation.Transactional;
import com.pa.evs.dto.GroupDto;
import com.pa.evs.dto.PaginDto;

public interface GroupService {
    
    void addGroupDevice(GroupDto dto) throws IOException;

    void getGroupDevies(PaginDto<GroupDto> dto);

    @Transactional
    void deleteGroupDevice(Long id) throws Exception;
}
