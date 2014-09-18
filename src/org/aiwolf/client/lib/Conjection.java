package org.aiwolf.client.lib;

public enum Conjection {
	AND,
	OR;


	public static Conjection fromString(String text){
		if (text != null) {
			for (Conjection c : Conjection.values()) {
				if (text.equalsIgnoreCase(c.name())) {
					return c;
				}
			}
		}
		return null;
	}

}
