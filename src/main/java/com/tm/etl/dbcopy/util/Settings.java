package com.tm.etl.dbcopy.util;

import com.beust.jcommander.Parameter;
import lombok.ToString;

@ToString
class Settings {
  @Parameter(names = {"-h", "--help", "--usage"}, description = "Usage.", help = true)
  boolean help;

  @Parameter(names = {"-c", "--configuration"}, description = "Configuration file name.", required = true)
  String configurationFileName;

  @Parameter(names = {"-p", "--properties"}, description = "Properties file name.")
  String propertiesFileName;

  @Parameter(names = {"-e", "--withEnvironment"}, description = "Use environment variables as substites.")
  boolean withEnvironment;
}
