package com.pa.evs.dto;

import com.pa.evs.enums.JasperFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Getter
@Setter
public class ExportReportDto {

    private Long reportId;
    private JasperFormat format;
    private List<ReportJasperParameterDto> parameters;

}
