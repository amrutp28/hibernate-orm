/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.function.Supplier;

import org.hibernate.Incubating;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;

import static org.hibernate.internal.log.IncubationLogger.INCUBATION_LOGGER;

/**
 * Collection of helper methods for dealing with configuration settings.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class ConfigurationHelper {

	private static final String PLACEHOLDER_START = "${";

	/**
	 * Disallow instantiation
	 */
	private ConfigurationHelper() {
	}

	/**
	 * Get the config value as a {@link String}
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value, or null if not found
	 */
	public static String getString(String name, Map values) {
		Object value = values.get( name );
		if ( value == null ) {
			return null;
		}
		return value.toString();
	}

	/**
	 * Get the config value as a {@link String}
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 * @param defaultValue The default value to use if not found
	 *
	 * @return The value.
	 */
	public static String getString(String name, Map values, String defaultValue) {
		return getString( name, values, () -> defaultValue );
	}

	/**
	 * Get the config value as a {@link String}
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value, or null if not found
	 */
	public static String getString(String name, Map<?,?> values, Supplier<String> defaultValueSupplier) {
		final Object value = values.get( name );
		if ( value != null ) {
			return value.toString();
		}

		return defaultValueSupplier.get();
	}

	/**
	 * Get the config value as a {@link String}.
	 *
	 * @param name The config setting name.
	 * @param values The map of config parameters.
	 * @param defaultValue The default value to use if not found.
	 * @param otherSupportedValues List of other supported values. Does not need to contain the default one.
	 *
	 * @return The value.
	 *
	 * @throws ConfigurationException Unsupported value provided.
	 *
	 */
	public static String getString(String name, Map values, String defaultValue, String ... otherSupportedValues) {
		final String value = getString( name, values, defaultValue );
		if ( !defaultValue.equals( value ) && ArrayHelper.indexOf( otherSupportedValues, value ) == -1 ) {
			throw new ConfigurationException(
					"Unsupported configuration [name=" + name + ", value=" + value + "]. " +
							"Choose value between: '" + defaultValue + "', '" + String.join( "', '", otherSupportedValues ) + "'."
			);
		}
		return value;
	}

	/**
	 * Get the config value as a boolean (default of false)
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value.
	 */
	public static boolean getBoolean(String name, Map values) {
		return getBoolean( name, values, false );
	}

	/**
	 * Get the config value as a boolean.
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 * @param defaultValue The default value to use if not found
	 *
	 * @return The value.
	 */
	public static boolean getBoolean(String name, Map values, boolean defaultValue) {
		final Object raw = values.get( name );

		final Boolean value = toBoolean( raw, defaultValue );
		if ( value == null ) {
			throw new ConfigurationException(
					"Could not determine how to handle configuration raw [name=" + name + ", value=" + raw + "] as boolean"
			);
		}

		return value;
	}

	public static Boolean toBoolean(Object value, boolean defaultValue) {
		if ( value == null ) {
			return defaultValue;
		}

		if (value instanceof Boolean) {
			return (Boolean) value;
		}

		if (value instanceof String) {
			return Boolean.parseBoolean( (String) value );
		}

		return null;
	}

	/**
	 * Get the config value as a boolean (default of false)
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value.
	 */
	public static Boolean getBooleanWrapper(String name, Map values, Boolean defaultValue) {
		Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof String) {
			return Boolean.valueOf( (String) value );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name + ", value=" + value + "] as boolean"
		);
	}

	/**
	 * Get the config value as an int
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 * @param defaultValue The default value to use if not found
	 *
	 * @return The value.
	 */
	public static int getInt(String name, Map values, int defaultValue) {
		Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof String) {
			return Integer.parseInt( (String) value );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name +
						", value=" + value + "(" + value.getClass().getName() + ")] as int"
		);
	}

	/**
	 * Get the config value as an {@link Integer}
	 *
	 * @param name The config setting name.
	 * @param values The map of config values
	 *
	 * @return The value, or null if not found
	 */
	public static Integer getInteger(String name, Map values) {
		Object value = values.get( name );
		if ( value == null ) {
			return null;
		}
		if (value instanceof Integer) {
			return (Integer) value;
		}
		if (value instanceof String) {
			//empty values are ignored
			final String trimmed = value.toString().trim();
			if ( trimmed.isEmpty() ) {
				return null;
			}
			return Integer.valueOf( trimmed );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name +
						", value=" + value + "(" + value.getClass().getName() + ")] as Integer"
		);
	}

	public static long getLong(String name, Map values, int defaultValue) {
		Object value = values.get( name );
		if ( value == null ) {
			return defaultValue;
		}
		if (value instanceof Long) {
			return (Long) value;
		}
		if (value instanceof String) {
			return Long.parseLong( (String) value );
		}
		throw new ConfigurationException(
				"Could not determine how to handle configuration value [name=" + name +
						", value=" + value + "(" + value.getClass().getName() + ")] as long"
		);
	}

	/**
	 * Make a clone of the configuration values.
	 *
	 * @param configurationValues The config values to clone
	 *
	 * @return The clone
	 */
	@SuppressWarnings("unchecked")
	public static Map clone(Map<?,?> configurationValues) {
		if ( configurationValues == null ) {
			return null;
		}
		// If a Properties object, leverage its clone() impl
		if (configurationValues instanceof Properties) {
			return (Properties) ( (Properties) configurationValues ).clone();
		}
		// Otherwise make a manual copy
		HashMap clone = new HashMap();
		for ( Map.Entry entry : configurationValues.entrySet() ) {
			clone.put( entry.getKey(), entry.getValue() );
		}
		return clone;
	}



	/**
	 * replace a property by a starred version
	 *
	 * @param props properties to check
	 * @param key property to mask
	 *
	 * @return cloned and masked properties
	 */
	public static Properties maskOut(Properties props, String key) {
		Properties clone = ( Properties ) props.clone();
		if ( clone.get( key ) != null ) {
			clone.setProperty( key, "****" );
		}
		return clone;
	}





	/**
	 * Extract a property value by name from the given properties object.
	 * <p>
	 * Both {@code null} and {@code empty string} are viewed as the same, and return null.
	 *
	 * @param propertyName The name of the property for which to extract value
	 * @param properties The properties object
	 * @return The property value; may be null.
	 */
	public static String extractPropertyValue(String propertyName, Properties properties) {
		String value = properties.getProperty( propertyName );
		if ( value == null ) {
			return null;
		}
		value = value.trim();
		if ( value.isEmpty() ) {
			return null;
		}
		return value;
	}
	/**
	 * Extract a property value by name from the given properties object.
	 * <p>
	 * Both {@code null} and {@code empty string} are viewed as the same, and return null.
	 *
	 * @param propertyName The name of the property for which to extract value
	 * @param properties The properties object
	 * @return The property value; may be null.
	 */
	public static String extractPropertyValue(String propertyName, Map properties) {
		String value = (String) properties.get( propertyName );
		if ( value == null ) {
			return null;
		}
		value = value.trim();
		if ( value.isEmpty() ) {
			return null;
		}
		return value;
	}

	public static String extractValue(
			String name,
			Map values,
			Supplier<String> fallbackValueFactory) {
		final String value = extractPropertyValue( name, values );
		if ( value != null ) {
			return value;
		}

		return fallbackValueFactory.get();
	}

	/**
	 * Constructs a map from a property value.
	 * <p>
	 * The exact behavior here is largely dependant upon what is passed in as
	 * the delimiter.
	 *
	 * @see #extractPropertyValue(String, Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param delim The string defining tokens used as both entry and key/value delimiters.
	 * @param properties The properties object
	 * @return The resulting map; never null, though perhaps empty.
	 */
	public static Map toMap(String propertyName, String delim, Properties properties) {
		Map map = new HashMap();
		String value = extractPropertyValue( propertyName, properties );
		if ( value != null ) {
			StringTokenizer tokens = new StringTokenizer( value, delim );
			while ( tokens.hasMoreTokens() ) {
				map.put( tokens.nextToken(), tokens.hasMoreElements() ? tokens.nextToken() : "" );
			}
		}
		return map;
	}

	/**
	 * Constructs a map from a property value.
	 * <p>
	 * The exact behavior here is largely dependant upon what is passed in as
	 * the delimiter.
	 *
	 * @see #extractPropertyValue(String, Properties)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param delim The string defining tokens used as both entry and key/value delimiters.
	 * @param properties The properties object
	 * @return The resulting map; never null, though perhaps empty.
	 */
	public static Map toMap(String propertyName, String delim, Map properties) {
		Map map = new HashMap();
		String value = extractPropertyValue( propertyName, properties );
		if ( value != null ) {
			StringTokenizer tokens = new StringTokenizer( value, delim );
			while ( tokens.hasMoreTokens() ) {
				map.put( tokens.nextToken(), tokens.hasMoreElements() ? tokens.nextToken() : "" );
			}
		}
		return map;
	}

	/**
	 * Get a property value as a string array.
	 *
	 * @see #extractPropertyValue(String, Properties)
	 * @see #toStringArray(String, String)
	 *
	 * @param propertyName The name of the property for which to retrieve value
	 * @param delim The delimiter used to separate individual array elements.
	 * @param properties The properties object
	 * @return The array; never null, though may be empty.
	 */
	public static String[] toStringArray(String propertyName, String delim, Properties properties) {
		return toStringArray( extractPropertyValue( propertyName, properties ), delim );
	}

	/**
	 * Convert a string to an array of strings.  The assumption is that
	 * the individual array elements are delimited in the source stringForm
	 * param by the delim param.
	 *
	 * @param stringForm The string form of the string array.
	 * @param delim The delimiter used to separate individual array elements.
	 * @return The array; never null, though may be empty.
	 */
	public static String[] toStringArray(String stringForm, String delim) {
		// todo : move to StringHelper?
		if ( stringForm != null ) {
			return StringHelper.split( delim, stringForm );
		}
		else {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}
	}

	/**
	 * Handles interpolation processing for all entries in a properties object.
	 *
	 * @param configurationValues The configuration map.
	 */
	public static void resolvePlaceHolders(Map<?,?> configurationValues) {
		Iterator itr = configurationValues.entrySet().iterator();
		while ( itr.hasNext() ) {
			final Map.Entry entry = ( Map.Entry ) itr.next();
			final Object value = entry.getValue();
			if (value instanceof String) {
				final String resolved = resolvePlaceHolder( ( String ) value );
				if ( !value.equals( resolved ) ) {
					if ( resolved == null ) {
						itr.remove();
					}
					else {
						entry.setValue( resolved );
					}
				}
			}
		}
	}

	/**
	 * Handles interpolation processing for a single property.
	 *
	 * @param property The property value to be processed for interpolation.
	 * @return The (possibly) interpolated property value.
	 */
	public static String resolvePlaceHolder(String property) {
		if ( property.indexOf( PLACEHOLDER_START ) < 0 ) {
			return property;
		}
		StringBuilder buff = new StringBuilder();
		char[] chars = property.toCharArray();
		for ( int pos = 0; pos < chars.length; pos++ ) {
			if ( chars[pos] == '$' ) {
				// peek ahead
				if ( chars[pos+1] == '{' ) {
					// we have a placeholder, spin forward till we find the end
					String systemPropertyName = "";
					int x = pos + 2;
					for (  ; x < chars.length && chars[x] != '}'; x++ ) {
						systemPropertyName += chars[x];
						// if we reach the end of the string w/o finding the
						// matching end, that is an exception
						if ( x == chars.length - 1 ) {
							throw new IllegalArgumentException( "unmatched placeholder start [" + property + "]" );
						}
					}
					String systemProperty = extractFromSystem( systemPropertyName );
					buff.append( systemProperty == null ? "" : systemProperty );
					pos = x + 1;
					// make sure spinning forward did not put us past the end of the buffer...
					if ( pos >= chars.length ) {
						break;
					}
				}
			}
			buff.append( chars[pos] );
		}
		String rtn = buff.toString();
		return rtn.isEmpty() ? null : rtn;
	}

	private static String extractFromSystem(String systemPropertyName) {
		try {
			return System.getProperty( systemPropertyName );
		}
		catch( Throwable t ) {
			return null;
		}
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForBoolean(StandardServiceRegistry serviceRegistry) {
		final Integer typeCode = serviceRegistry.getService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_BOOLEAN_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( typeCode != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_BOOLEAN_JDBC_TYPE );
			return typeCode;
		}

		// default to the Dialect answer
		return serviceRegistry.getService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect()
				.getPreferredSqlTypeCodeForBoolean();
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForDuration(StandardServiceRegistry serviceRegistry) {
		final Integer explicitSetting = serviceRegistry.getService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_DURATION_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( explicitSetting != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_DURATION_JDBC_TYPE );
			return explicitSetting;
		}

		return SqlTypes.NUMERIC;
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForUuid(StandardServiceRegistry serviceRegistry) {
		final Integer explicitSetting = serviceRegistry.getService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_UUID_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( explicitSetting != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_UUID_JDBC_TYPE );
			return explicitSetting;
		}

		return SqlTypes.UUID;
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForInstant(StandardServiceRegistry serviceRegistry) {
		final Integer explicitSetting = serviceRegistry.getService( ConfigurationService.class ).getSetting(
				AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE,
				TypeCodeConverter.INSTANCE
		);
		if ( explicitSetting != null ) {
			INCUBATION_LOGGER.incubatingSetting( AvailableSettings.PREFERRED_INSTANT_JDBC_TYPE );
			return explicitSetting;
		}

		return SqlTypes.TIMESTAMP_UTC;
	}

	@Incubating
	public static synchronized int getPreferredSqlTypeCodeForArray(StandardServiceRegistry serviceRegistry) {
		// default to the Dialect answer
		return serviceRegistry.getService( JdbcServices.class )
				.getJdbcEnvironment()
				.getDialect()
				.getPreferredSqlTypeCodeForArray();
	}

	private static class TypeCodeConverter implements ConfigurationService.Converter<Integer> {

		public static final TypeCodeConverter INSTANCE = new TypeCodeConverter();

		@Override
		public Integer convert(Object value) {
			if ( value == null ) {
				throw new IllegalArgumentException( "Null value passed to convert" );
			}

			if ( value instanceof Number ) {
				return ( (Number) value ).intValue();
			}

			final String string = value.toString().toUpperCase( Locale.ROOT );
			final Integer typeCode = JdbcTypeNameMapper.getTypeCode( string );
			if ( typeCode != null ) {
				return typeCode;
			}
			try {
				return Integer.parseInt( string );
			}
			catch (NumberFormatException ex) {
				throw new IllegalArgumentException( String.format( "Couldn't interpret '%s' as JDBC type code or type code name", string ) );
			}
		}
	}
}
