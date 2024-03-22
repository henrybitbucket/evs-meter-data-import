package com.pa.evs.sv;

import java.util.List;

import com.pa.evs.dto.DMSApplicationGuestSaveReqDto;
import com.pa.evs.dto.DMSApplicationSaveReqDto;
import com.pa.evs.dto.DMSProjectDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;

@SuppressWarnings("rawtypes")
public interface DMSProjectService {

	void save(DMSProjectDto dto) throws ApiException;

	void search(PaginDto<DMSProjectDto> pagin);

	void delete(Long id) throws ApiException;

	void update(DMSProjectDto dto) throws ApiException;

	void searchPicUsers(PaginDto pagin);

	void searchLocations(PaginDto pagin);

	void linkPicUser(String email, Long projectId);

	void searchSubPicUserInProject(PaginDto pagin, Long projectId);

	void searchDMSPicUsers(PaginDto pagin);

	void linkSite(Long projectId, List<Long> siteIds);

	void linkSubPicUsers(List<String> emails, Long projectId);

	Object submitApplication(Long projectId, DMSApplicationSaveReqDto dto);

	Object submitApplication(Long projectId, DMSApplicationGuestSaveReqDto dto);

	void approveApplication(Long applicationId);

	void rejectApplication(Long applicationId);

	void deleteApplication(Long applicationId);
}
