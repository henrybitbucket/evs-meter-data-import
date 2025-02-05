package com.pa.evs.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(
	name = "p2_report_ack", 
	indexes = {
		@Index(columnList = "user_submit", name = "idx_meter_commissioning_report_user_submit"),
		@Index(columnList = "user_submit,uid", name = "idx_meter_commissioning_report_user_submit_uid"),
		@Index(columnList = "user_submit,msn", name = "idx_meter_commissioning_report_user_submit_msn"),
		@Index(columnList = "uid,is_latest", name = "idx_meter_commissioning_report_uid_is_latest"),
		@Index(columnList = "user_submit,job_sheet_no", name = "idx_meter_commissioning_report_user_submit_job_sheet_no"),
		@Index(columnList = "job_by,job_sheet_no", name = "idx_meter_commissioning_report_job_by_job_sheet_no")
	}
)
public class P2ReportAck extends BaseEntity {

	private String uid;
	private String sn;
	private String cid;
	private String msn;
	private Boolean isPassed;
    private String kwh;
    private String kw;
    private String i;
    private String v;
    private String pf;
	private Long dt;
	
	@Column(name = "meter_photos", length = 20000)
	private String meterPhotos;

	@Column(name = "status")
	@Enumerated(EnumType.STRING)
	private DeviceStatus status;

	@Column(name = "d_type")
	@Enumerated(EnumType.STRING)
	private DeviceType type;

	@Column(name = "last_OBR_date")
	private Long lastOBRDate;
	
	@Column(name = "is_latest", columnDefinition = "boolean not null default false")
	private Boolean isLatest;

	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE })
	@JoinColumn(name = "installer")
	private Users installer;
	
	@Column(name = "user_submit")
	private String userSubmit;
	
	@Column(name = "time_submit")
	private Long timeSubmit;
	
	@Column(name = "comment_submit")
	private String commentSubmit;

	@Column(name = "job_sheet_no")
	private String jobSheetNo;

	@Column(name = "job_by")
	private String jobBy;

	private String coupledUser;
	
	@Column(name = "manager_submit")
	private String managerSubmit;
	
	@Column(name = "contract_order", length = 255)
	private String contractOrder;
	
}
