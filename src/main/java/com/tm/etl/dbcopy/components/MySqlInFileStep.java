package com.tm.etl.dbcopy.components;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Setter
public class MySqlInFileStep extends SqlStep {
  private ResultSetInputStream stream;

  @Override
  public PreparedStatement prepareStatement(Db db, String sql) throws SQLException, ClassNotFoundException {
    log.debug("Enabling MySQL streaming");
    PreparedStatement preparedStatement = db.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    preparedStatement.setFetchSize(Integer.MIN_VALUE);
    return preparedStatement;
  }

  @Override
  public void load(PreparedStatement preparedStatement, SqlStep previousStep) throws SQLException {
    log.debug("Loading with MySQL setLocalInfileInputStream()");

    // TODO: => com.mysql.cj.jdbc.PreparedStatement

    if (!(preparedStatement instanceof com.mysql.jdbc.PreparedStatement)) {
      throw new RuntimeException("MySqlInFileStep can only be used with MySQL. " + preparedStatement.getClass().getName());
    }

    stream.setInputResultSet(previousStep.getResultSet());
    ((com.mysql.jdbc.PreparedStatement) preparedStatement).setLocalInfileInputStream(stream);
    preparedStatement.execute();
  }
}
