// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package oql.actions;

import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.webui.CustomJavaAction;
import oql.implementation.OQL;

public class AddDecimalParameter extends CustomJavaAction<java.lang.Boolean>
{
	private final java.lang.String name;
	private final java.math.BigDecimal value;

	public AddDecimalParameter(
		IContext context,
		java.lang.String _name,
		java.math.BigDecimal _value
	)
	{
		super(context);
		this.name = _name;
		this.value = _value;
	}

	@java.lang.Override
	public java.lang.Boolean executeAction() throws Exception
	{
		// BEGIN USER CODE
		OQL.addParameter(name, value);
		return true;
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 * @return a string representation of this action
	 */
	@java.lang.Override
	public java.lang.String toString()
	{
		return "AddDecimalParameter";
	}

	// BEGIN EXTRA CODE
	// END EXTRA CODE
}
