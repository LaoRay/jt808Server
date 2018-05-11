package com.clubank.position.jt808.vo;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 位置实体
 */
@Getter
@Setter
@ToString
public class GpsLocations {

	private Long id;

	private String terminalNo;

	private Float latitude;

	private Float longitude;

	private Integer elevation;

	private Float speed;

	private Integer direction;

	private Date time;
}