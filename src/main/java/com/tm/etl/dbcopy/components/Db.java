package com.tm.etl.dbcopy.components;

import com.tm.etl.dbcopy.util.DriverShim;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Data
@Slf4j
public class Db {
  private String name;
  private String driver;
  private String url;
  private String user;
  private String pass;
  private String path;

  private int resultSetDefaultType = ResultSet.TYPE_FORWARD_ONLY;
  private int resultSetDefaultConcurrency = ResultSet.CONCUR_READ_ONLY;

  Connection connection = null;

  private Connection connect() throws ClassNotFoundException, SQLException {
    if (connection != null)
      return connection;

    if ((path != null) && !"".equals(path)) {
      try {
        String spec = "jar:file:" + path + "!/";
        URL u = new URL(spec);
        URLClassLoader ucl = new URLClassLoader(new URL[]{u});
        Driver d = (Driver) Class.forName(driver, true, ucl).newInstance();
        DriverManager.registerDriver(new DriverShim(d));
      } catch (Exception e) {
        throw new RuntimeException("Failed to load driver: " + path, e);
      }
    } else if ((driver != null) && !"".equals(driver)) {
      Class.forName(driver);
    }
    connection = DriverManager.getConnection(url, System.getenv(user), System.getenv(pass));
    connection.setAutoCommit(false);
    return connection;
  }

  public void commit() throws SQLException, ClassNotFoundException {
    log.info("Commit: " + name);
    connect().commit();
  }

  public PreparedStatement prepareStatement(String sql) throws SQLException, ClassNotFoundException {
    return prepareStatement(sql, resultSetDefaultType, resultSetDefaultConcurrency);
  }

  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
    throws SQLException, ClassNotFoundException {
    log.info("Executing '" + name + "': " + sql);
    return connect().prepareStatement(sql, resultSetType, resultSetConcurrency);
  }
}
