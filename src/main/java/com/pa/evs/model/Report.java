package com.pa.evs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Table;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "evs_reports")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class Report extends BaseEntity {

    private String reportName;

    private byte[] binBlob;

}
