package com.tm.etl.dbcopy.util;

import com.beust.jcommander.JCommander;
import com.tm.etl.dbcopy.DbCopy;
import com.tm.etl.dbcopy.components.SqlStep;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class VelocityLoader<T> {

  private final Class<T> type;

  public VelocityLoader(Class<T> type) {
    this.type = type;
  }

  @SuppressWarnings("unchecked")
  public Optional<T> getLoader(String[] args) throws IOException {
    T loader = null;
    Settings settings = new Settings();
    JCommander jCommander = new JCommander(settings);
    jCommander.parse(args);
    if (settings.help) {
      jCommander.setProgramName("java -jar " + DbCopy.dbCopyJar);
      jCommander.usage();
    } else {
      Yaml yaml = new Yaml();
      Reader reader;
      Velocity.init();
      VelocityContext context;
      Map<String, String> map = new HashMap<>();
      if (settings.withEnvironment) {
        // Load env to Velocity context
        map.putAll(System.getenv());
      }
      if (settings.propertiesFileName != null) {
        // Read properties file to reader
        reader = new FileReader(new File(settings.propertiesFileName));
        if (settings.withEnvironment) {
          // Substitute properties with env
          reader = new StringReader(VelocityLoader.evaluate(new VelocityContext(map), reader));
        }
        // Load properties to Velocity context
        map.putAll(yaml.loadAs(reader, Map.class));
      }
      // Read configuration to reader
      reader = new FileReader(new File(settings.configurationFileName));
      // Substitute configuration with env and properties
      context = new VelocityContext(map);
      reader = new StringReader(VelocityLoader.evaluate(context, reader));
      // Load DbCopy
      loader = yaml.loadAs(reader, type);
      context.put("ConfigurationFileName", settings.configurationFileName);
      context.put("PropertiesFileName", settings.propertiesFileName);
      context.put("DidRunWithEnvironment", settings.withEnvironment);
      SqlStep.context = context;
      if (log.isTraceEnabled()) {
        for (Object k : context.getKeys()) {
          log.trace("{} = {}", k, context.get((String) k));
        }
      }
    }
    return Optional.ofNullable(loader);
  }

  public static String evaluate(VelocityContext context, Reader reader) {
    StringWriter stringWriter = new StringWriter();
    String logTag = "eval";
    Velocity.evaluate(context, stringWriter, logTag, reader);
    return stringWriter.toString();
  }
}
