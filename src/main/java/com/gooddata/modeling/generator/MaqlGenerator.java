package com.gooddata.modeling.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gooddata.modeling.model.SourceColumn;
import com.gooddata.modeling.model.SourceSchema;
import com.gooddata.naming.N;
import com.gooddata.util.StringUtil;

/**
 * GoodData MAQL Generator generates the MAQL from the LDM schema object
 *
 * @author zd <zd@gooddata.com>
 * @version 1.0
 */
public class MaqlGenerator {

    private final SourceSchema schema;
    private final String ssn, lsn;
	private final String factsOfAttrMaqlDdl;

    private Map<String, Attribute> attributes = new HashMap<String, Attribute>();
    private List<Fact> facts = new ArrayList<Fact>();
    private List<Label> labels = new ArrayList<Label>();
    private List<DateColumn> dates = new ArrayList<DateColumn>();
    private List<Reference> references = new ArrayList<Reference>();
    private boolean hasCp = false;

    public MaqlGenerator(SourceSchema schema) {
        this.schema = schema;
        this.ssn = StringUtil.formatShortName(schema.getName());
        this.lsn = StringUtil.formatLongName(schema.getName());
        this.factsOfAttrMaqlDdl = createFactOfMaqlDdl(schema.getName());
    }

	/**
     * Generates the MAQL from the schema
     *
     * @return the MAQL as a String
     */
    public String generateMaql() {
        String script = "";

        script += "CREATE DATASET {dataset." + ssn + "} VISUAL(TITLE \"" + lsn + "\");\n\n";
        script += generateFoldersMaqlDdl(schema.getColumns());

        // generate attributes and facts
        for (SourceColumn column : schema.getColumns()) {
            processColumn(column);
        }

        for (final Column c : attributes.values()) {
            script += c.generateMaqlDdl();
        }
        for (final Column c : facts) {
            script += c.generateMaqlDdl();
        }
        for (final Column c : dates) {
            script += c.generateMaqlDdl();
        }
        for (final Column c : references) {
        	script += c.generateMaqlDdl();
        }


        // generate the facts of / record id special attribute
        script += "CREATE ATTRIBUTE " + factsOfAttrMaqlDdl + " VISUAL(TITLE \""
                  + "Records of " + lsn + "\") AS KEYS {" + getFactTableName() + "."+N.ID+"} FULLSET;\n";
        script += "ALTER DATASET {dataset." + ssn + "} ADD {attr." + ssn + ".factsof};\n\n";

        // labels last
        for (final Column c : labels) {
            script += c.generateMaqlDdl();
        }
        
        // finally synchronize
        script += "SYNCHRONIZE {dataset." + ssn + "};";
        return script;
    }

    private String generateFoldersMaqlDdl(List<SourceColumn> columns) {
        final ArrayList<String> attributeFolders = new ArrayList<String>();
        final ArrayList<String> factFolders = new ArrayList<String>();

        for (SourceColumn column : columns) {
            String folder = column.getFolder();
            if (folder != null && folder.length() > 0) {
                if (column.getLdmType().equalsIgnoreCase(SourceColumn.LDM_TYPE_ATTRIBUTE) ||
                        column.getLdmType().equalsIgnoreCase(SourceColumn.LDM_TYPE_LABEL)) {
                    if (!attributeFolders.contains(folder))
                        attributeFolders.add(folder);
                }
                if (column.getLdmType().equalsIgnoreCase(SourceColumn.LDM_TYPE_FACT)) {
                    if (!factFolders.contains(folder))
                        factFolders.add(folder);
                }
            }
        }

        String script = "";
        // Generate statements for the ATTRIBUTE folders
        for (String folder : attributeFolders) {
            String sfn = StringUtil.formatShortName(folder);
            String lfn = StringUtil.formatLongName(folder);
            script += "CREATE FOLDER {dim." + sfn + "} VISUAL(TITLE \"" + lfn + "\") TYPE ATTRIBUTE;\n";
        }
        script += "\n";

        // Generate statements for the FACT folders
        for (String folder : factFolders) {
            String sfn = StringUtil.formatShortName(folder);
            String lfn = StringUtil.formatLongName(folder);
            script += "CREATE FOLDER {ffld." + sfn + "} VISUAL(TITLE \"" + lfn + "\") TYPE FACT;\n";
        }

        script += "\n";
        return script;
    }

    private void processColumn(SourceColumn column) {
        if (column.getLdmType().equals(SourceColumn.LDM_TYPE_ATTRIBUTE)) {
        	Attribute attr = new Attribute(column);
            attributes.put(attr.scn, attr);
        } else if (column.getLdmType().equals(SourceColumn.LDM_TYPE_FACT)) {
            facts.add(new Fact(column));
        } else if (column.getLdmType().equals(SourceColumn.LDM_TYPE_DATE)) {
            dates.add(new DateColumn(column));
        } else if (column.getLdmType().equals(SourceColumn.LDM_TYPE_LABEL)) {
            labels.add(new Label(column));
        } else if (column.getLdmType().equals(SourceColumn.LDM_TYPE_REFERENCE)) {
        	references.add(new Reference(column));
        } else
            processConnectionPoint(column);
    }

    private void processConnectionPoint(SourceColumn column) {
        if (column.getLdmType().equals(SourceColumn.LDM_TYPE_CONNECTION_POINT)) {
            if (hasCp) {
                throw new IllegalStateException("Only one connection point per dataset is allowed. "
                        + "Consider declaring the duplicate connection points as labels of the main connection point.");
            }
            ConnectionPoint connectionPoint = new ConnectionPoint(column);
            attributes.put(connectionPoint.scn, connectionPoint);
        }
    }
    
    private String getFactTableName() {
    	return N.FCT_PFX + ssn;
    }
    
    private static String createFactOfMaqlDdl(String schemaName) {
    	return "{attr." + StringUtil.formatShortName(schemaName) + "." + N.FACTS_OF + "}";
	}
    
    // column entities

    private abstract class Column {
        protected final SourceColumn column;
        protected final String scn, lcn;

        Column(SourceColumn column) {
            this.column = column;
            this.scn = StringUtil.formatShortName(column.getName());
            this.lcn = StringUtil.formatLongName(column.getTitle());
        }

        protected String createForeignKeyMaqlDdl() {
           	return "{" + getFactTableName() + "." + scn + "_" + N.ID+ "}";
        }
        
        public abstract String generateMaqlDdl();
    }

    private class Attribute extends Column {

        protected final String table;
        protected final String identifier;

        Attribute(SourceColumn column, String table) {
            super(column);
            this.identifier = "attr." + ssn + "." + scn;
            this.table = (table == null) ? "d_" + ssn + "_" + scn : table;
        }

        Attribute(SourceColumn column) {
            this(column, null);
        }

        @Override
        public String generateMaqlDdl() {
            String folderStatement = "";
            String folder = column.getFolder();
            if (folder != null && folder.length() > 0) {
                String sfn = StringUtil.formatShortName(folder);
                folderStatement = ", FOLDER {dim." + sfn + "}";
            }

            String script = "CREATE ATTRIBUTE {" + identifier + "} VISUAL(TITLE \"" + lcn
                    + "\"" + folderStatement + ") AS KEYS {" + table + "."+N.ID+"} FULLSET, ";
            script += createForeignKeyMaqlDdl();
            script += " WITH LABELS {label." + ssn + "." + scn + "} VISUAL(TITLE \""
                    + lcn + "\") AS {d_" + ssn + "_" + scn + "."+N.NM_PFX + scn + "};\n"
                    + "ALTER DATASET {dataset." + ssn + "} ADD {attr." + ssn + "." + scn + "};\n\n";
            return script;
        }
    }

    private class Fact extends Column {

        Fact(SourceColumn column) {
            super(column);
        }

        @Override
        public String generateMaqlDdl() {
            String folderStatement = "";
            String folder = column.getFolder();
            if (folder != null && folder.length() > 0) {
                String sfn = StringUtil.formatShortName(folder);
                folderStatement = ", FOLDER {ffld." + sfn + "}";
            }

            return "CREATE FACT {fact." + ssn + "." + scn + "} VISUAL(TITLE \"" + lcn
                    + "\"" + folderStatement + ") AS {" + getFactTableName() + "."+N.FCT_PFX + scn + "};\n"
                    + "ALTER DATASET {dataset." + ssn + "} ADD {fact." + ssn + "." + scn + "};\n\n";
        }
    }

    private class Label extends Column {

        Label(SourceColumn column) {
            super(column);
        }

        @Override
        public String generateMaqlDdl() {
            String scnPk = StringUtil.formatShortName(column.getReference());
            Attribute attr = attributes.get(scnPk);
            
            if (attr == null) {
            	throw new IllegalArgumentException("Label " + scn + " points to non-existing attribute " + scnPk);
            }

            return "ALTER ATTRIBUTE {attr." + ssn + "." + scnPk + "} ADD LABELS {label." + ssn + "." + scnPk + "."
                    + scn + "} VISUAL(TITLE \"" + lcn + "\") AS {" + attr.table + "."+N.NM_PFX + scn + "};\n\n";
        }
    }

    private class DateColumn extends Column {

        DateColumn(SourceColumn column) {
            super(column);
        }

        @Override
        public String generateMaqlDdl() {
            String folderStatement = "";
            String folder = column.getFolder();
            String reference = column.getReference();
            if (folder != null && folder.length() > 0) {
                String sfn = StringUtil.formatShortName(folder);
                folderStatement = ", FOLDER {ffld." + sfn + "}";
            }
            String stat = "CREATE FACT {"+N.DT+"." + ssn + "." + scn + "} VISUAL(TITLE \"" + lcn
                    + "\"" + folderStatement + ") AS {" + getFactTableName() + "."+N.DT_PFX + scn + "_"+N.ID+"};\n"
                    + "ALTER DATASET {dataset." + ssn + "} ADD {"+N.DT+"." + ssn + "." + scn + "};\n\n";
            if(reference != null && reference.length() > 0) {
                stat += "ALTER ATTRIBUTE {"+reference+"."+N.DT_ATTR_NAME+"} ADD KEYS {"+getFactTableName() + 
                        "."+N.DT_PFX + scn + "_"+N.ID+"};\n\n";
            }
            return stat;

        }
    }

    private class ConnectionPoint extends Attribute {
        public ConnectionPoint(SourceColumn column) {
            super(column);
            hasCp = true;
        }

		 @Override
		protected String createForeignKeyMaqlDdl() {
			// The fact table's primary key values are identical with the primary key values
			// of a Connection Point attribute. This is why the fact table's PK may act as 
			// the connection point's foreign key as well
			return "{" + getFactTableName() + "."+N.ID+"}";
		}
    }
    
    private class Reference extends Column {
    	public Reference(SourceColumn column) {
			super(column);
		}
    	
    	@Override
    	public String generateMaqlDdl() {
    		String foreignAttrId = createFactOfMaqlDdl(column.getSchemaReference());
    		String script = "ALTER ATTRIBUTE " + foreignAttrId
    					  + " ADD KEYS " + createForeignKeyMaqlDdl() + ";\n"; 
    		return script;
    	}
    }
    
}

