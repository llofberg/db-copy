package com.tm.etl.dbcopy.components;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;

import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import static com.tm.etl.dbcopy.util.VelocityLoader.evaluate;

@Data
@Slf4j
public class SqlStep {
  public static VelocityContext context;

  protected String name = "SqlStep";
  protected String sql;
  private Db db;

  private boolean hasResult = false;
  private boolean doCommit = true;
  protected int batchLogCount = 1000000;

  private boolean toContext = false;

  private boolean debug = false;
  private boolean dump = false;

  protected PreparedStatement preparedStatement = null;
  protected ResultSet resultSet = null;
  protected int columnCount = 0;
  private int batchSize = 1000;
  public boolean okToFail = false;
  private boolean stopWhenNoInput = false;

  public void run(SqlStep previousStep) throws SQLException, ClassNotFoundException {
    log.info("{}: {}", name, getClass().getName());
    String sql = evaluate(context, new StringReader(this.sql));
    PreparedStatement preparedStatement = prepareStatement(db, sql);
    if (hasResult) {
      if ((previousStep != null) && (previousStep.resultSet != null)) {
        previousStep.resultSet.next();
        preparedStatement = setParameters(preparedStatement, previousStep);
      }
      resultSet = preparedStatement.executeQuery();
      columnCount = resultSet.getMetaData().getColumnCount();
    } else {
      if (previousStep == null || (!previousStep.hasResult)) {
        preparedStatement.execute();
      } else {
        int batchCount = load(preparedStatement, previousStep);
        if (batchCount == 0 && stopWhenNoInput) {
          throw new RuntimeException("Input was expected but none was available.");
        }else{
          log.info("BatchCount: {}", batchCount);
        }
      }
      if (doCommit) {
        db.commit();
      }
    }
    if (previousStep != null) {
      if (previousStep.resultSet != null)
        previousStep.resultSet.close();
      if (previousStep.preparedStatement != null)
        previousStep.preparedStatement.close();
    }
    if (dump && resultSet != null) {
      dump(resultSet);
      resultSet.close();
      resultSet = null;
    }
    if (toContext && resultSet != null) {
      toContext(resultSet);
      resultSet.close();
      resultSet = null;
    }
  }

  public int load(PreparedStatement preparedStatement, SqlStep previousStep) throws SQLException, ClassNotFoundException {
    int batchCount = 0;
    while (previousStep.resultSet.next()) {
      preparedStatement = setParameters(preparedStatement, previousStep);
      preparedStatement.addBatch();
      if ((++batchCount) % batchSize == 0)
        executeBatch(preparedStatement, batchCount);
    }
    if ((batchCount) % batchLogCount != 0) {
      executeBatch(preparedStatement, batchCount);
    }
    log.info("Total rows: " + batchCount);
    return batchCount;
  }

  private void executeBatch(PreparedStatement preparedStatement, int batchCount) throws SQLException, ClassNotFoundException {
    if ((batchCount) % batchLogCount == 0)
      log.info("executeBatch(" + batchCount + ")");
    try {
      int[] res = preparedStatement.executeBatch();
      for (int i = 0; i < res.length; i++) {
        if (res[i] == Statement.EXECUTE_FAILED) {
          log.error(String.format("Failed to insert row %d.", i));
        }
      }
      db.commit();
      preparedStatement.clearBatch();
    } catch (SQLException e) {
      logNestedSqlExceptions(e);
      throw e;
    }
  }

  private void logNestedSqlExceptions(SQLException e) {
    while (e != null) {
      log.error(e.getMessage());
      e = e.getNextException();
    }
  }

  public PreparedStatement prepareStatement(Db db, String sql) throws SQLException, ClassNotFoundException {
    return db.prepareStatement(sql);
  }

  private PreparedStatement setParameters(PreparedStatement preparedStatement, SqlStep previousStep) throws SQLException {
    for (int i = 1; i <= previousStep.columnCount; i++) {
      Object object = previousStep.resultSet.getObject(i);
      int columnType = previousStep.resultSet.getMetaData().getColumnType(i);
      if (object instanceof Boolean) {
        columnType = Types.INTEGER;
        Boolean b = (Boolean) object;
        object = b ? 1 : 0;
      }
      if (previousStep.resultSet.wasNull()) {
        if (debug) {
          log.debug("{}: {} {}", i, null, previousStep.resultSet.getMetaData().getColumnTypeName(i));
        }
        preparedStatement.setNull(i, columnType);
      } else {
        if (debug) {
          log.debug("{}: {} {}", i, object, previousStep.resultSet.getMetaData().getColumnTypeName(i));
        }
        preparedStatement.setObject(i, object, columnType);
      }
    }
    return preparedStatement;
  }

  private void toContext(ResultSet resultSet) {
    try {
      resultSet.next();
      ResultSetMetaData metaData = resultSet.getMetaData();
      for (int i = 1; i <= metaData.getColumnCount(); i++) {
        Object object = resultSet.getObject(i);
        if (resultSet.wasNull()) {
          object = null;
        }
        context.put(metaData.getColumnLabel(i), object);
        if (debug) {
          log.debug("To context: {}: {} {} {}", i,
            metaData.getColumnLabel(i),
            metaData.getColumnTypeName(i),
            object);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to dump()", e);
    }
  }

  private void dump(ResultSet resultSet) {
    try {
      while (resultSet.next()) {
        ResultSetMetaData metaData = resultSet.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
          Object object = resultSet.getObject(i);
          if (resultSet.wasNull()) {
            object = null;
          }
          log.debug("DUMP: {}: {} {} {}", i,
            metaData.getColumnLabel(i),
            metaData.getColumnTypeName(i),
            object);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to dump()", e);
    }
  }
}
