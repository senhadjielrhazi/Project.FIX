package org.neurosystem.util.log;

public enum Priority {
	  /**
	     The <code>OFF</code> has the highest possible rank and is
	     intended to turn off logging.  */
	  OFF(7),

	  /**
	     The <code>FATAL</code> level designates very severe error
	     events that will presumably lead the application to abort.
	   */
	  FATAL(6),

	  /**
	     The <code>ERROR</code> level designates error events that
	     might still allow the application to continue running.  */
	  ERROR(5),

	  /**
	     The <code>WARN</code> level designates potentially harmful situations.
	  */
	  WARN(4),

	  /**
	     The <code>INFO</code> level designates informational messages
	     that highlight the progress of the application at coarse-grained
	     level.  */
	  INFO(3),

	  /**
	     The <code>DEBUG</code> Level designates fine-grained
	     informational events that are most useful to debug an
	     application.  */
	  DEBUG(2),

	  /**
	    * The <code>TRACE</code> Level designates finer-grained
	    * informational events than the <code>DEBUG</code level.
	   *  @since 1.2.12
	    */
	  TRACE(1),


	  /**
	     The <code>ALL</code> has the lowest possible rank and is intended to
	     turn on all logging.  */
	  ALL(0);
	  
	  private int p_level;
	  
	  Priority(int level){
		  this.p_level = level;
	  }
	  
	  public int getLevel(){
		  return this.p_level;
	  }
	  
	  public boolean isAllowed(Priority level){
		  if(getLevel() <= level.getLevel()){
			  return true;
		  }
		  
		  return false;
	  }
}
