package org.aiwolf.client.lib;

import java.util.ArrayList;

public class LogicalStracture {

	private ArrayList<String> element = new ArrayList<String>();
	private Conjection c = null;

	public static LogicalStracture splitStracture(String text){

		LogicalStracture logicalStracture = new LogicalStracture();
		ArrayList<String> e = new ArrayList<String>();
		String[] split = text.split("\\s+");
		if(!(split[0].equals("("))){
			e.add(text);
		}else{
			int count = 0;
			int start=0, end=0;
			for(int i = 0; i < split.length; i++){
				if(split[i].equals("(")){
					count++;
					if(count == 1){
						start = i + 1;
					}
				}else if(split[i].equals(")")){
					count--;
					if(count == 0){
						end = i - 1;
					}
				}
				if(count == 0){
					if(logicalStracture.getC() == null){
						logicalStracture.setC(Conjection.fromString(split[i+1]));
					}
					String[] elementSubset = new String[end - start + 1];
					for(int j = start; j <= end; j++){
						elementSubset[j - start] = split[j];
					}
					e.add(bondStrings(elementSubset, " "));
					i++;//接続詞でcount == 0となる時に，このif文をを飛ばすため．
				}

			}
		}
		logicalStracture.setElement(e);
		return logicalStracture;
	}

	/**
	 * 分割されたStringをbondでくっつける．
	 * @param split
	 * @param bond
	 * @return
	 */
	public static String bondStrings(String[] split, String bond){
		String text = "";
		for(String s: split){
			text = text + s + bond;
		}
		return text.substring(0, text.length()-bond.length());
	}

	public static String bondStrings(ArrayList<String> split, String bond){
		String[] split2 = new String[split.size()];
		for(int i = 0; i < split2.length; i++){
			split2[i] = split.get(i);
		}
		return LogicalStracture.bondStrings(split2, bond);
	}

	public ArrayList<String> getElement() {
		return element;
	}

	public void setElement(ArrayList<String> element) {
		this.element = element;
	}

	public Conjection getC() {
		return c;
	}

	public void setC(Conjection c) {
		this.c = c;
	}
}
