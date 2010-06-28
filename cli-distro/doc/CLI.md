

Project Management Commands:
----------------------------

* CreateProject(name=<project-name>, desc=<description>) - create a new project on the <hostname> server
  project-name - name of the new project
  description  - (optional) project description

* DropProject(id=<project-id>) - drop the project on the <hostname> server
  project-id - project id

* OpenProject(id=<identifier>) - open an existing project for data modeling and data upload. Equivalent to providing the project identifier using the "-e" command line option.
  identifier - id of an existing project (takes the form of an MD5 hash)

* StoreProject(fileName=<file>) - saves the current project identifier into the specified file
  fileName - file to save the project identifier
  
* RetrieveProject(fileName=<file>) - loads the current project identifier from the specified file
  fileName - file to load the project identifier from

 Lock(path=<file>) - prevents concurrent run of multiple instances sharing the same lock file. Lock files older than 1 hour are discarded.

Logical Model Management Commands:
----------------------------------

* GenerateMaql(maqlFile=<maql>) - generate MAQL DDL script describing data model from the local config file, must call CreateProject or OpenProject and a LoadXXX before
  maqlFile - path to MAQL file (will be overwritten)
  
* GenerateUpdateMaql(maqlFile=<maql>) - generate MAQL DDL alter script that creates the columns available in the local configuration but missing in the remote GoodData project, must call CreateProject or OpenProject and LoadXXX before
  maqlFile - path to MAQL file (will be overwritten)

* ExecuteMaql(maqlFile=<maql> [, ifExists=<true | false>]) - run MAQL DDL script on server to generate data model, must call CreateProject or OpenProject and LoadXXX before
  maqlFile - path to the MAQL file (relative to PWD)
  ifExists - if set to true the command quits silently if the maqlFile does not exist, default is false

Data Transfer Commands:
-----------------------

* TransferData([incremental=<true | false>] [, waitForFinish=<true | false>]) - upload data (all snapshots) to the server, must call CreateProject or OpenProject and Load<Connector> before
  incremental - incremental transfer (true | false), default is false
  waitForFinish - waits for the server-side processing (true | false), default is true

* TransferSnapshots(firstSnapshot=snapshot-id, lastSnapshot=snapshot-id [,incremental=<true | false>] [, waitForFinish=<true | false>]) - uploads all snapshots between the firstSnapshot and the lastSnapshot (inclusive). 
  firstSnapshot - the first transferred snapshot id
  lastSnapshot - the last transferred snapshot id
  incremental - incremental transfer (true | false), default is false
  waitForFinish - waits for the server-side processing (true | false), default is true

* TransferData([incremental=<true | false>] [, waitForFinish=<true | false>]) - uploads the lastSnapshot 
  incremental - incremental transfer (true | false), default is false
  waitForFinish - waits for the server-side processing (true | false), default is true

* ListSnapshots() - list all data snapshots from the internal DB, must call CreateProject or OpenProject before

* DropSnapshots() - drops the internal DB, must call CreateProject or OpenProject before

CSV Connector Commands:
-----------------------

* GenerateCsvConfig(csvHeaderFile=<file>, configFile=<config> [, defaultLdmType=<type>] [, folder=<folder>]) - generate a sample XML config file based on the fields from your CSV file. If the config file exists already, only new columns are added. The config file must be edited as the LDM types (attribute | fact | label etc.) are assigned randomly.
  csvHeaderFile - path to CSV file (only the first header row will be used)
  configFile  - path to configuration file (will be overwritten)
  defaultLdmType - LDM type to be associated with new columns (only ATTRIBUTE type is supported by the ProcessNewColumns task at this time)
  folder - folder where to place new attributes

* LoadCsv(csvDataFile=<data>, configFile=<config>, header=<true | false>) - load CSV data file using config file describing the file structure, must call CreateProject or OpenProject before
  csvDataFile    - path to CSV datafile
  configFile  - path to XML configuration file (see the GenerateCsvConfig command that generates the config file template)
  header - true if the CSV file has header in the first row, false otherwise 

GoogleAnalytics Connector Commands:
-----------------------------------

* GenerateGoogleAnalyticsConfig(name=<name>, configFile=<config>, dimensions=<pipe-separated-ga-dimensions>, metrics=<pipe-separated-ga-metrics>) - generate an XML config file based on the fields from your GA query.
  name - the new dataset name
  configFile  - path to configuration file (will be overwritten)
  dimensions - pipe (|) separated list of Google Analytics dimensions (see http://code.google.com/apis/analytics/docs/gdata/gdataReferenceDimensionsMetrics.html)
  metrics - pipe (|) separated list of Google Analytics metrics (see http://code.google.com/apis/analytics/docs/gdata/gdataReferenceDimensionsMetrics.html)


* LoadGoogleAnalytics(configFile=<config>, username=<ga-username>, password=<ga-password>, profileId=<ga-profile-id>, dimensions=<pipe-separated-ga-dimensions>, metrics=<pipe-separated-ga-metrics>, startDate=<date>, endDate=<date>, filters=<ga-filter-string>)  - load GA data file using config file describing the file structure, must call CreateProject or OpenProject before
  configFile  - path to configuration file (will be overwritten)
  username - Google Analytics username
  password - Google Analytics password
  profileId - Google Analytics profile ID (this is a value of the id query parameter in the GA url)
  dimensions - pipe (|) separated list of Google Analytics dimensions (see http://code.google.com/apis/analytics/docs/gdata/gdataReferenceDimensionsMetrics.html)
  metrics - pipe (|) separated list of Google Analytics metrics (see http://code.google.com/apis/analytics/docs/gdata/gdataReferenceDimensionsMetrics.html)
  startDate - the GA start date in the yyyy-mm-dd format  
  endDate - the GA end date in the yyyy-mm-dd format  
  filters - the GA filters (see http://code.google.com/apis/analytics/docs/gdata/gdataReferenceDataFeed.html#filters)

JDBC Connector Commands:
------------------------

* GenerateJdbcConfig(name=<name>, configFile=<config>, driver=<jdbc-driver>, url=<jdbc-url>, query=<sql-query> [, username=<jdbc-username>] [, password=<jdbc-password>])  - generate an XML config file based on the fields from your JDBC query.
  name - the new dataset name
  configFile  - path to configuration file (will be overwritten)
  driver - JDBC driver string (e.g. "org.apache.derby.jdbc.EmbeddedDriver"), you'll need to place the JAR with the JDBC driver to the lib subdirectory
  url - JDBC url (e.g. "jdbc:derby:mydb")
  query - SQL query (e.g. "SELECT employee,dept,salary FROM payroll")
  username - JDBC username
  password - JDBC password
  
* LoadJdbc(configFile=<config>, driver=<jdbc-driver>, url=<jdbc-url>, query=<sql-query> [, username=<jdbc-username>] [, password=<jdbc-password>])  - load JDBC data file using config file describing the file structure, must call CreateProject or OpenProject before
  configFile  - path to configuration file (will be overwritten)
  driver - JDBC driver string (e.g. "org.apache.derby.jdbc.EmbeddedDriver"), you'll need to place the JAR with the JDBC driver to the lib subdirectory
  url - JDBC url (e.g. "jdbc:derby:mydb")
  query - SQL query (e.g. "SELECT employee,dept,salary FROM payroll")
  username - JDBC username
  password - JDBC password

SalesForce Connector Commands:
------------------------------

* GenerateSfdcConfig(name=<name>, configFile=<config>, query=<soql-query>, username=<sfdc-username>, password=<sfdc-password>, token=<sfdc-security-token>)  - generate an XML config file based on the fields from your SFDC query.
  name - the new dataset name
  configFile  - path to configuration file (will be overwritten)
  query - SOQL query (e.g. "SELECT Id, Name FROM Account"), see http://www.salesforce.com/us/developer/docs/api/Content/data_model.htm
  username - SFDC username
  password - SFDC password
  token - SFDC security token (you may append the security token to the password instead using this parameter)
  
* LoadSfdc(configFile=<config>, query=<soql-query>, username=<sfdc-username>, password=<sfdc-password>, token=<sfdc-security-token>)  - load SalesForce data file using config file describing the file structure, must call CreateProject or OpenProject before
  configFile  - path to configuration file (will be overwritten)
  query - SOQL query (e.g. "SELECT Id, Name FROM Account"), see http://www.salesforce.com/us/developer/docs/api/Content/data_model.htm
  username - SFDC username
  password - SFDC password
  token - SFDC security token (you may append the security token to the password instead using this parameter)
  

Time Dimension Connector:
-------------------------

* LoadDateDimension(name=<name>)  - load new time dimension into the project, must call CreateProject or OpenProject before
  name - the time dimension name differentiates the time dimension form others. This is typically something like "closed", "created" etc.