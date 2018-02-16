package com.tm.etl.dbcopy;

import ch.vorburger.mariadb4j.DB;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class LoaderTest {

  @Test
  public void test() throws Exception {
    log.info("Setting up test databases");
    int port = getPort();

    DB database = DB.newEmbeddedDB(port);
    database.start();

    Map<String, String> env = new HashMap<>();
    env.put("SECRET", "P$SS");
    env.put("PORT", "" + port);
    set(env);
    log.info("Setting up test databases done");

    DbCopy.main(new String[]{"-c", "src/test/resources/etl.yml", "-e"});
  }

  private int getPort() throws IOException {
    ServerSocket s = new ServerSocket(0);
    int port = s.getLocalPort();
    s.close();
    return port;
  }

  private static void set(Map<String, String> newEnv) throws Exception {
    set(newEnv, false);
  }

  @SuppressWarnings("rawtypes")
  private static void set(Map<String, String> newEnv, boolean clear) throws Exception {
    Class[] classes = Collections.class.getDeclaredClasses();
    Map<String, String> env = System.getenv();
    for (Class cl : classes) {
      if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
        Field field = cl.getDeclaredField("m");
        field.setAccessible(true);
        Object obj = field.get(env);
        @SuppressWarnings("unchecked")
        Map<String, String> map = (Map<String, String>) obj;
        if (clear) {
          map.clear();
        }
        map.putAll(newEnv);
      }
    }
  }
}
