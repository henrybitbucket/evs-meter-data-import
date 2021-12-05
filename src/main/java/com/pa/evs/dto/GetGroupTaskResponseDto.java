package com.pa.evs.dto;


import com.pa.evs.model.GroupTask;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GetGroupTaskResponseDto {
    private List<GroupTask> results;
}
