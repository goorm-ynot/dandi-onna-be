package com.mvp.v1.dandionna.s3.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author rua
 */
@Getter
@Setter
@AllArgsConstructor
public class S3Metadata {
	String key;
	String etag;
	String contentType;
}
