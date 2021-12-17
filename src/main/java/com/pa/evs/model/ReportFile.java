package com.pa.evs.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import java.sql.Blob;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "report_file")
public class ReportFile extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "report_task_id")
    private ReportTask reportTask;
    
    @Lob
   	@Column(name = "bin_Blob")
    private Blob binBlob;
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_format")
    private String fileFormat;

}
