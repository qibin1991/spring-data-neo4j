package org.springframework.data.neo4j.core.mapping;

import org.springframework.data.mapping.MappingException;

public class NoRootNodeMappingException extends MappingException {

	public NoRootNodeMappingException(String s) {
		super(s);
	}
}
