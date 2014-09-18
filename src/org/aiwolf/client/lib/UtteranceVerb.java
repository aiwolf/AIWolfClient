package org.aiwolf.client.lib;

/**
 * 発話文の動詞を表す
 * declare, request, agreeの3種類(2014/06/26時点)
 * @author kengo
 *
 */
public enum UtteranceVerb {
	declare, request, agree;

	public static UtteranceVerb fromString(String text) {
		if (text != null) {
			for (UtteranceVerb b : UtteranceVerb.values()) {
				if (text.equalsIgnoreCase(b.name())) {
					return b;
				}
			}
		}
		return null;
	}

}
