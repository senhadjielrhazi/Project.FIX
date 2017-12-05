package org.neurosystem.modules.neuroscience.dna;

public enum IGene {
	U('U', 1.),
	F('F', 0.),
	D('D',-1.);
	
	private final char code;
	private final double value;
	
	private IGene(char code, double value){
		this.code = code;
		this.value = value;
	}
	
	public char code(){
		return this.code;
	}
	
	public double value(){
		return this.value;
	}
	
	public static IGene fromCode(char code){
		if(code == U.code)
			return U;
		
		if(code == D.code)
			return D;
		
		if(code == F.code)
			return F;

		return null;
	}
	
	public static IGene fromValue(double value){
		if(value == U.value)
			return U;
	
		if(value == D.value)
			return D;
		
		if(value == F.value)
			return F;
		
		return null;
	}
}