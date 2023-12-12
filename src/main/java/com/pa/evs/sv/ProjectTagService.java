package com.pa.evs.sv;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.ProjectTagDto;

public interface ProjectTagService {

	void save(ProjectTagDto dto);

	void delete(Long tagId);

	PaginDto<ProjectTagDto> search(PaginDto<ProjectTagDto> pagin);

}
