db-copy
=======

A simple ETL tool to copy data between JDBC databases.

          Usage: java -jar db-copy-<VERSION>.jar [options]
            Options:
            * -c, --configuration
                Configuration file name.
              -p, --properties
                Properties file name.
              -e, --withEnvironment
                Use environment variables as substites.
                Default: false
              -h, --help, --usage
                Usage.

Configuration is a YAML file with optional template placeholders that are substituted
from the property file and optionally from environment variables.

Property file is also a YAML file with optional template placeholders that are optionally
substituted from environment variables.

Configuration consists of two lists; ```dbs``` and ```steps```.

First list,```dbs``` lists databases data is copied from and to.

          dbs:
          - &HSQL_DB
            name: "HSql"
            user: "sa"
            pass: ${HSQL_PASSWORD}
            driver: "org.hsqldb.jdbc.JDBCDriver"
            url: "jdbc:hsqldb:mem:testw"
            jars:
              - "jar:https://repo1.maven.org/maven2/org/hsqldb/hsqldb/2.3.1/hsqldb-2.3.1.jar!/"
            autoCommit: false

          - &MARIA_DB
            name: "MariaDB4j"
            user: "root"
            pass: ${MARIA_PASSWORD}
            url: "jdbc:mysql://localhost:${PORT}/test"


| Parameter  | Default | Description                               | Example                              |
|------------|---------|-------------------------------------------|--------------------------------------|
| name       |         | Descriptive name                          | Development HSql                     |
| user       |         | Database user name                        | super_user                           |
| pass       |         | Database password                         | super_secret or ${PASSWORD}          |
| driver     |         | Database JDBC driver class name           | org.hsqldb.jdbc.JDBCDriver           |
| url        |         | Database JDCB URL                         | jdbc:hsqldb:mem:test                 |
| jars       |         | List of URL's to JDBC driver jar files    | - "jar:./drivers/hsqldb-2.3.1.jar!/" |
| autocommit |  true   | Automatically commit after each statement | true                                 |

Second list, ```steps``` lists the operations to be executed.

          steps:
          # Log version and environment variable (be careful!)
          - db: *MARIA_DB
            name: "Maria 1"
            sql: "SELECT '${dbCopyJar}', '${SECRET}'"
          # Create test tables
          - db: *HSQL_DB
            name: "HSql 2"
            sql: "CREATE TABLE HOO(hoo VARCHAR(256), ts TIMESTAMP(0))"
	etc.

| Parameter     | Default | Description                                                   |             Example |
|---------------|---------|---------------------------------------------------------------|--------------------:|
| name          |         | Descriptive name.                                             |          Sales pull |
| sql           |         | SQL statement.                                                | select a,b,c from d |
| db            |         | Database "YAML" reference.                                    |           *MARIA_DB |
| batchSize     | 1000    | The size of a batch sent to the database when inserting data. |               10000 |
| hasResult     | false   | Indicates that the statement will return a result.            |                true |
| doCommit      | true    | Should commit be called after running the statement.          |                true |
| batchLogCount | 1000000 | How often to log progress of processing the data.             |               10000 |
| toContext     | false   | Place results of the statement to the context as variables.   |                true |
| debug         | false   | Print the data s it is being processed.                       |                true |
| dump          | false   | Dump all data the statement returns. Used for debugging.      |                true |

See [test scripts](src/test/resources/) for more examples.

When a steps hasResult is true, the next step can use the results as input.
The order of columns in both the first step and the next step must match as well as the number of questionmark placeholders.

          # Select three columns from HOO
          - db: *HSQL_DB
            name: "HSql 5"
            hasResult: true
            sql: "select id, amount, ts from HOO"

          # Insert three columns in the same order as above to MOO
          - db: *MARIA_DB
            name: "Maria 6"
            sql: "INSERT INTO MOO(id, amount, ts)values(?, ?, ?)" # Note the three questionmark placeholders

When a steps toContext is true, the following steps can use the results first rows data as variables.

          - db: *HSQL_DB
            name: "HSql 5"
            hasResult: true
			toContext: true
            sql: "select sum(amount) AS TOTAL_AMOUNT from HOO"

          - db: *MARIA_DB
            name: "Maria 6"
            sql: "INSERT INTO TOTAL_MOO(amount, ts)values(${TOTAL_AMOUNT}, current_timestamp)"

Be careful with SQL injections here...
