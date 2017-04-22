package com.toddfast.mutagen.cassandra.mutation;

import com.toddfast.mutagen.MutagenException;

import java.io.File;

public class MutationParser {
	
	private static final int SUBJECT_INDEX = 0;
	
	private static final int VERSION_INDEX = 1;
	
	public static final Integer parseMutationVersion(String resourcePath) {
		return Integer.parseInt(parseFilename(resourcePath)[VERSION_INDEX]);
	}
	
	public static final String parseMutationSubject(String resourcePath) {
		return parseFilename(resourcePath)[SUBJECT_INDEX];
	}
	
	private static final String[] parseFilename(String resourcePath) {
		File resource = new File(resourcePath);
		String[] fileNameParts = resource.getName().split("\\.")[0].split("_");
		if(fileNameParts.length < 2) {
			throw new MutagenException("File: " + resourcePath + " naming scheme is malformed.");
		}
		return fileNameParts;
	}
}
