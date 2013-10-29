package org.montrealtransit.android.provider.common;

public final class SqlUtils {

	public static final String CREATE_TABLE = "CREATE TABLE ";
	public static final String CREATE_TABLE_IF_NOT_EXIST = CREATE_TABLE + "IF NOT EXISTS ";
	public static final String DROP_TABLE = "DROP TABLE ";
	public static final String DROP_TABLE_IF_EXISTS = DROP_TABLE + "IF EXISTS ";

	public static final String INT = " integer";
	public static final String INT_PK = INT + " PRIMARY KEY";
	public static final String INT_PK_AUTO = INT_PK + " AUTOINCREMENT";

	public static final String TXT = " text";

	public static final String REAL = " real";

	public static final String INNER_JOIN = " INNER JOIN ";
	public static final String LEFT_OUTER_JOIN = " LEFT OUTER JOIN ";
	public static final String FULL_OUTER_JOIN = " FULL OUTER JOIN ";

	public static String getSQLDropIfExistsQuery(String table) {
		return DROP_TABLE_IF_EXISTS + table;
	}

	public static String getSQLForeignKey(String columnName, String fkTable, String fkColumn) {
		return " FOREIGN KEY(" + columnName + ") REFERENCES " + fkTable + "(" + fkColumn + ")";
	}

	private SqlUtils() {
	}

}
