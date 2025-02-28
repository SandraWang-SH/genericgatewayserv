package com.paypal.raptor.aiml.model;

import lombok.Getter;
import lombok.Setter;


/**
 * @author: qingwu
 **/
@Getter
public class FileContent {

	public FileContent(String filename, byte[] file) {
		this.filename = filename;
		this.file = file;
	}

	private String filename;

	private byte[] file;
}
