# This example demonstrates how to load more complex structures to GoodData
# Three datasets: department, employee, and salary are loaded are connected together
# Check the configuration files that contains the connection points and references

# Create a new project
CreateProject(name="HR");

# Store the project ID to file for the subsequent two scripts
StoreProject(fileName="examples/hr/pid");

# Load the department data file, using the XML file describing the data
LoadCsv(csvDataFile="examples/hr/department.csv",header="true",configFile="examples/hr/department.xml");

# Generate the MAQL script describing data model for department data
GenerateMaql(maqlFile="examples/hr/department.maql");

# Execute the MAQL script on the server
ExecuteMaql(maqlFile="examples/hr/department.maql");

# Transfer the department data
TransferLastSnapshot(incremental="false");