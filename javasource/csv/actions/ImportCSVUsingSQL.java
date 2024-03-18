// This file was generated by Mendix Studio Pro.
//
// WARNING: Only the following code will be retained when actions are regenerated:
// - the import list
// - the code between BEGIN USER CODE and END USER CODE
// - the code between BEGIN EXTRA CODE and END EXTRA CODE
// Other code you write will be lost the next time you deploy the project.
// Special characters, e.g., é, ö, à, etc. are supported in comments.

package csv.actions;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaObject;
import com.mendix.systemwideinterfaces.core.meta.IMetaPrimitive;
import com.mendix.webui.CustomJavaAction;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import csv.impl.CSV;

/**
 * Imports a full CSV dataset using optimized SQL batches.
 * It's very fast and has no impact on memory consumption and able to handle millions of records, but is limited to full loads of simple structures (no associations or inheritance).
 * 
 * Attributes will be found based on headers (and spaces will be replaced by _).
 * Non matching attributes won't be imported (warning of skipped headers will be present).
 * 
 * Tested target data types:
 *  - String
 *  - Integer
 *  - Long
 *  - DateTime (from unix timestamp including ms)
 *  - Enumeration
 *  - Boolean (accepted: true TRUE false FALSE 0 1)
 * 
 * Only tested for PostgreSQL!
 */
public class ImportCSVUsingSQL extends CustomJavaAction<java.lang.Long>
{
	/** @deprecated use file.getMendixObject() instead. */
	@java.lang.Deprecated(forRemoval = true)
	private final IMendixObject __file;
	private final system.proxies.FileDocument file;
	private final java.lang.String separator;
	private final java.lang.String quoteChar;
	private final java.lang.Long skipLines;
	private final java.lang.String targetEntity;
	private final java.lang.String characterSet;
	private final java.lang.String decimalSeparator;
	private final java.lang.String groupingSeparator;

	public ImportCSVUsingSQL(
		IContext context,
		IMendixObject _file,
		java.lang.String _separator,
		java.lang.String _quoteChar,
		java.lang.Long _skipLines,
		java.lang.String _targetEntity,
		java.lang.String _characterSet,
		java.lang.String _decimalSeparator,
		java.lang.String _groupingSeparator
	)
	{
		super(context);
		this.__file = _file;
		this.file = _file == null ? null : system.proxies.FileDocument.initialize(getContext(), _file);
		this.separator = _separator;
		this.quoteChar = _quoteChar;
		this.skipLines = _skipLines;
		this.targetEntity = _targetEntity;
		this.characterSet = _characterSet;
		this.decimalSeparator = _decimalSeparator;
		this.groupingSeparator = _groupingSeparator;
	}

	@java.lang.Override
	public java.lang.Long executeAction() throws Exception
	{
		// BEGIN USER CODE
		Long result = Core.dataStorage().executeWithConnection(getContext(), executeConnectionAction());
		logger.info("Imported " + result + " records...");
		return result;
		// END USER CODE
	}

	/**
	 * Returns a string representation of this action
	 * @return a string representation of this action
	 */
	@java.lang.Override
	public java.lang.String toString()
	{
		return "ImportCSVUsingSQL";
	}

	// BEGIN EXTRA CODE
	private static ILogNode logger = CSV.getLogger();
	
	private Function<Connection, Long> executeConnectionAction() {
		return connection -> {
			try {
				CSVParserBuilder parserBuilder = new CSVParserBuilder()
						.withSeparator(this.separator.charAt(0)); 
				String[] attributes;
				
				if (this.quoteChar != null) {
					parserBuilder.withQuoteChar(this.quoteChar.charAt(0));
				}
				
				CSVParser parser = parserBuilder.build();
				CSVReaderBuilder readerBuilder = new CSVReaderBuilder(
						new InputStreamReader(Core.getFileDocumentContent(getContext(), this.file.getMendixObject()), 
						(this.characterSet != null ? this.characterSet : "UTF-8")
				));
				if (skipLines != 0) { 
					readerBuilder.withSkipLines(skipLines.intValue() - 1);
				}
				
				CSVReader reader =  readerBuilder			
						.withCSVParser(parser)
						.build();
				
				logger.debug("CSV file opened..");

				attributes = reader.readNext();
				
				IMetaObject metaObject = Core.getMetaObject(targetEntity);
				Map<String, IMetaPrimitive> columnMapping = new HashMap<>();
				
				for (int i = 0; i < attributes.length; i++) {
					IMetaPrimitive primitive = null;
					// search for a matching primitive in a case insensitive manner
					for (IMetaPrimitive prim : metaObject.getMetaPrimitives()) {
						if (prim.getName().equalsIgnoreCase(attributes[i].trim().replace(" ", "_"))) {
							primitive = prim;
						}
					}
					
					if (primitive == null || primitive.isVirtual()) {
						logger.warn("Attribute " + attributes[i] + " was not found in " + targetEntity + " and won't be imported.");
					} else {
						columnMapping.put(attributes[i], primitive);
					}
				}
								
				String[] line;
				List<String[]> buffer = new LinkedList<>();
				long counter = 0;
				while ((line = reader.readNext()) != null) {
					buffer.add(line);
					counter++;
					if (buffer.size() >= 10000) {
						importBatch(connection, targetEntity, attributes, columnMapping, buffer);
						buffer.clear();
						logger.info("Import progress: " + counter);
					}
					
				}
				importBatch(connection, targetEntity, attributes, columnMapping, buffer);

				reader.close();
				
				return counter;
			} catch (Exception e) {
				logger.error("Error while importing CSV..", e);
				throw new RuntimeException(e);
			}
		};
	}
	
	private Long getNextIDForBatch(String entity, int size) {
		IContext context = Core.createSystemContext();
		Long id = Core.dataStorage().executeWithConnection(context, executeGetNextIDForBatch(context, entity, size));
		context.endTransaction();
		return id;
	}
	
	private Function<Connection, Long> executeGetNextIDForBatch(IContext context, String entity, int size) {
		return connection -> {
			final String getSequenceQuery = "SELECT id, object_sequence FROM mendixsystem$entityidentifier WHERE " +
						"id = (SELECT id FROM mendixsystem$entity WHERE entity_name = ?) FOR UPDATE";
			final String setSequenceQuery = "UPDATE mendixsystem$entityidentifier SET object_sequence = ? WHERE id = ?";
			
			try {
				Long start =  ((long) Core.getMetaObject(entity).getId()) << 48;
				
				PreparedStatement getStat = connection.prepareStatement(getSequenceQuery);
				getStat.setString(1, entity);
				ResultSet getRes = getStat.executeQuery();
				getRes.next();
				String id = getRes.getString(1);
				Long result = getRes.getLong(2);
				getStat.close();
				getRes.close();
				
				PreparedStatement setStat = connection.prepareStatement(setSequenceQuery);
				setStat.setLong(1, result + size);
				setStat.setString(2, id);
				setStat.executeUpdate();
				setStat.close();
				
				connection.close();
				
				return result + start;
				 
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		};
	}
	
	private Boolean importBatch(Connection connection, String entity, String[] attributes, 
			Map<String, IMetaPrimitive> columnMapping, List<String[]> buffer) throws Exception {
		IMetaObject metaObject = Core.getMetaObject(entity);
		String table =  Core.getDatabaseTableName(metaObject);
		DecimalFormat df = CSV.getDecimalFormat(decimalSeparator, groupingSeparator);

		Long id = getNextIDForBatch(metaObject.getName(), buffer.size());
		String insertQuery = "INSERT INTO " + table + " (id";
		
		for (String attribute : attributes) {
			if (columnMapping.containsKey(attribute)) {
				insertQuery += ", " + Core.getDatabaseColumnName(metaObject.getDeclaredMetaPrimitive(attribute));
			}
		}
		insertQuery += ") VALUES (?";
		for (String attribute : attributes) {
			if (columnMapping.containsKey(attribute)) {
				insertQuery += ", ?";
			}
		}
		insertQuery += ")";
		
		logger.debug("Insert query: " + insertQuery);
		
		PreparedStatement insertStatement = connection.prepareStatement(insertQuery);
		for (String[] line : buffer) {
			insertStatement.setLong(1, id);
			
			int offset = 1;
			for (int i = 0; i < attributes.length; i++) {
				if (!columnMapping.containsKey(attributes[i])) continue; // header does not lead to an attribute
				offset++;
				IMetaPrimitive primitive = columnMapping.get(attributes[i]);
				if (line[i] == null || line[i].isEmpty()) {
					insertStatement.setNull(offset, java.sql.Types.NULL);
					continue;
				}
				
				switch (primitive.getType()) {
				case String:
					insertStatement.setString(offset, line[i]);
					break;
				case Integer:
					insertStatement.setInt(offset, Integer.parseInt(line[i]));
					break;
				case Long:
					insertStatement.setBigDecimal(offset, (BigDecimal) df.parse(line[i]));
					break;
				case Enum:
					if (primitive.getEnumeration().getEnumValues().containsKey(line[i])) {
						insertStatement.setString(offset, line[i]);
					} else {
						throw new RuntimeException("Value " + line[i] + " is not a valid value for this enumeration.");
					}
					break;
				case DateTime:
					insertStatement.setTimestamp(offset, new java.sql.Timestamp(Long.parseLong(line[i])),
							Calendar.getInstance(TimeZone.getTimeZone("UTC")));
					break;
				case Boolean:
					insertStatement.setBoolean(offset, line[i].equalsIgnoreCase("true") || line[i].equals("1"));
					break;
				case Decimal:
					insertStatement.setBigDecimal(offset, (BigDecimal) df.parse(line[i]));
					break;
				default:
					throw new RuntimeException("Type "+ primitive.getType().toString() + " not supported.");
				}
			}
			insertStatement.addBatch();
			id++;
		}
		
		insertStatement.executeBatch();
		insertStatement.close();
					
		return true;
	}
	// END EXTRA CODE
}