// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package configuration.actions;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.webui.CustomJavaAction;
import org.joda.time.Chronology;
import org.joda.time.LocalDate;
import org.joda.time.chrono.IslamicChronology;
import org.joda.time.chrono.ISOChronology;
import java.time.format.DateTimeFormatter;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import java.util.*;
import java.time.*; 

public class GregorianToHijriDate extends CustomJavaAction<java.lang.String>
{
	private final java.util.Date GregorianDate;

	public GregorianToHijriDate(
		IContext context,
		java.util.Date _gregorianDate
	)
	{
		super(context);
		this.GregorianDate = _gregorianDate;
	}

	@java.lang.Override
	public java.lang.String executeAction() throws Exception
	{
		// BEGIN USER CODE

		/*int DDate = (int) (long) Date;
		int YYear = (int) (long) Year;
		int MMonth = (int) (long) Month;*/


		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(GregorianDate);

		int DDate = calendar.get(Calendar.DAY_OF_MONTH);
		int YYear = calendar.get(Calendar.YEAR);
		int MMonth = calendar.get(Calendar.MONTH);


		/*Core.getLogger("Gdate").info(calendar);
		Core.getLogger("date").info(DDate);
		Core.getLogger("month").info(MMonth);
		Core.getLogger("year").info(YYear);*/


		
		Chronology iso = ISOChronology.getInstance();
        Chronology hijri = IslamicChronology.getInstance();

        LocalDate todayIso = new LocalDate(YYear,(MMonth+1),DDate, iso);
        LocalDate todayHijri = new LocalDate(todayIso.toDateTimeAtStartOfDay(), hijri);


		Core.getLogger("Test").info(todayHijri);
		return todayHijri.toString();
						 
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 * @return a string representation of this action
	 */
	@java.lang.Override
	public java.lang.String toString()
	{
		return "GregorianToHijriDate";
	}

	// BEGIN EXTRA CODE
	// END EXTRA CODE
}