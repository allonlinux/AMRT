package fr.free.allonlinux.amrt.utils;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter
{
	@Override
	public String format(LogRecord i__logRecord)
	{
		String l__result="";
		
		if (i__logRecord.getLevel().equals(Level.SEVERE))
			l__result+="Error : ";
		
		if (i__logRecord.getLevel().equals(Level.WARNING))
			l__result+="Warning : ";
		
		l__result+=String.format(i__logRecord.getMessage()+"\n",i__logRecord.getParameters());
		
		return l__result;
	}
}
