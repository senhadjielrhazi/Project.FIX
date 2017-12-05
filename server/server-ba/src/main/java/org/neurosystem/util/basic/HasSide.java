package org.neurosystem.util.basic;

public interface HasSide {
	
	public enum Side {
		LONG,
		SHORT;
		
		public boolean isLong(){
	        return (this == LONG);
	    }
		
		public boolean isShort(){
	        return (this == SHORT);
	    }

		public Side flipsyde() {
			return isLong()?SHORT:LONG;
		}
	}
	
	public Side getSide();
}
