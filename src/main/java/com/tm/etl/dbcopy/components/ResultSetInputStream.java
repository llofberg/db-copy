package com.tm.etl.dbcopy.components;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Setter
public class ResultSetInputStream extends InputStream {
  //private char teradataFieldSep = 127;

  private String recordSeparator = "\n";
  private String columnSeparator = "\t";
  private String quoteCharacter = "\"";
  private String escapeCharacter = "!";

  private String recordSeparatorEscape = escapeCharacter + recordSeparator;
  private String columnSeparatorEscape = escapeCharacter + columnSeparator;
  private String quoteCharacterEscape = escapeCharacter + quoteCharacter;
  private String escapeEscape = escapeCharacter + escapeCharacter;

  private int batchLogCount = 1000;

  private ResultSet inputResultSet;
  private boolean debug = false;

  private int rowCount = 0;
  private int count = 0;
  private String data = null;

  public ResultSetInputStream() {

  }

  public ResultSetInputStream(int batchLogCount) {
    this.batchLogCount = batchLogCount;
  }

  @Override
  public int read() {
    try {
      if (data == null || data.length() == count) {
        if (inputResultSet.next()) {
          data = fetchData(inputResultSet);
          count = 0;
        } else {
          log.info("Total rows: " + rowCount);
          return -1;
        }
      }
      return data.charAt(count++);
    } catch (SQLException e) {
      e.printStackTrace();
      return -1;
    }
  }

  private String fetchData(ResultSet inputResultSet) throws SQLException {
    StringBuilder stringBuilder = new StringBuilder();
    int columnCount = inputResultSet.getMetaData().getColumnCount();
    String cs = "";
    for (int i = 1; i <= columnCount; i++) {
      String string = inputResultSet.getString(i);
      stringBuilder.append(cs);
      if (!inputResultSet.wasNull()) {
        string = processEscapes(string);
        stringBuilder.append(string);
      }
      cs = columnSeparator;
    }
    stringBuilder.append(recordSeparator);
    rowCount = logProgress(rowCount);
    String string = stringBuilder.toString();
    if (debug) {
      log.debug("{}: {}", rowCount, string);
    }
    return string;
  }

  private int logProgress(int rowCount) {
    if ((++rowCount) % batchLogCount == 0) {
      log.info("row: " + rowCount);
    }
    return rowCount;
  }

  private String processEscapes(String strValue) {
    strValue = strValue.replace(escapeCharacter, escapeEscape);
    strValue = strValue.replace(columnSeparator, columnSeparatorEscape);
    strValue = strValue.replace(recordSeparator, recordSeparatorEscape);
    strValue = strValue.replace(quoteCharacter, quoteCharacterEscape);
    return strValue;
  }

  public void setRecordSeparator(String recordSeparator) {
    this.recordSeparator = recordSeparator;
    this.recordSeparatorEscape = escapeCharacter + recordSeparator;
  }

  public void setColumnSeparator(String columnSeparator) {
    this.columnSeparator = columnSeparator;
    this.columnSeparatorEscape = escapeCharacter + columnSeparator;
  }

  public void setQuoteCharacter(String quoteCharacter) {
    this.quoteCharacter = quoteCharacter;
    this.quoteCharacter = escapeCharacter + quoteCharacter;
  }

  public void setEscapeCharacter(String escapeCharacter) {
    this.escapeCharacter = escapeCharacter;
    this.escapeEscape = escapeCharacter + escapeCharacter;
    this.recordSeparatorEscape = escapeCharacter + recordSeparator;
    this.columnSeparatorEscape = escapeCharacter + columnSeparator;
  }
}
