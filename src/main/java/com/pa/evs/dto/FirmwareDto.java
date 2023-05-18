package com.pa.evs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Getter
@Setter
public class FirmwareDto {

    private Long id;
    
    private String version;
    
    private String hashCode;
    
    private byte[] file;
    
    private String fileName;
    
    private VendorDto vendor;
}
