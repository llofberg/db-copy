package com.tm.etl.dbcopy;

import com.tm.etl.dbcopy.components.Db;
import com.tm.etl.dbcopy.components.SqlStep;
import com.tm.etl.dbcopy.util.VelocityLoader;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@Slf4j
@Data
public class DbCopy {

  public static final String dbCopyJar =
    new File(DbCopy.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();

  private ArrayList<Db> dbs;
  private ArrayList<SqlStep> steps;

  public static void main(String[] args) throws IOException {
    log.debug("dbCopyJar : {}", dbCopyJar);
    new VelocityLoader<>(DbCopy.class).getLoader(args).ifPresent(DbCopy::run);
  }

  private void run() {
    log.debug("{}: Start", dbCopyJar);
    SqlStep.context.put("dbCopyJar", dbCopyJar);
    validate(steps);
    SqlStep previousStep = null;
    for (SqlStep step : steps) {
      try {
        step.run(previousStep);
      } catch (Exception e) {
        log.error("Failed to run step '{}'", step.getName(), e);
        throw new RuntimeException("Failed to run step '" + step.getName() + "'", e);
      }
      previousStep = step;
    }
    log.debug("{}: Done", dbCopyJar);
  }

  private void validate(ArrayList<SqlStep> steps) {
    StringBuilder errors = new StringBuilder();
    for (SqlStep step : steps) {
      if (step.getDb() == null) {
        errors.append("\n  Missing DB  in step '").append(step.getName()).append("' ?");
      }
      if (step.getSql() == null || "".equals(step.getSql())) {
        errors.append("\n  Missing SQL in step '").append(step.getName()).append("' ?");
      }
    }
    if (errors.length() > 0) {
      throw new NullPointerException(errors.toString());
    }
  }
}
