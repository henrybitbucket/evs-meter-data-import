package com.pa.evs.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
@Setter
@Builder
public class LockDto {

    @Schema(description = "Set specific request timezone, example 'Asia/Bangkok' otherwise server timezone will be set")
    private String timeZone;

    private String bid;
    private String lockNumber;
}
