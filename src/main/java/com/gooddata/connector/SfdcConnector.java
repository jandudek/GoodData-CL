package com.gooddata.connector;

import au.com.bytecode.opencsv.CSVWriter;
import com.gooddata.exception.*;
import com.gooddata.modeling.model.SourceColumn;
import com.gooddata.modeling.model.SourceSchema;
import com.gooddata.exception.SfdcException;
import org.gooddata.processor.CliParams;
import org.gooddata.processor.Command;
import org.gooddata.processor.ProcessingContext;
import com.gooddata.util.FileUtil;
import com.gooddata.util.StringUtil;
import com.sforce.soap.partner.*;
import com.sforce.soap.partner.fault.*;
import com.sforce.soap.partner.sobject.SObject;
import org.apache.axis.message.MessageElement;
import org.apache.log4j.Logger;
import org.gooddata.connector.AbstractConnector;
import org.gooddata.connector.Connector;
import org.gooddata.connector.backend.ConnectorBackend;
import org.gooddata.processor.ProcessingContext;

import javax.xml.rpc.ServiceException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

/**
 * GoodData SFDC Connector
 *
 * @author zd <zd@gooddata.com>
 * @version 1.0
 */
public class SfdcConnector extends AbstractConnector implements Connector {

    private static Logger l = Logger.getLogger(SfdcConnector.class);

    private String sfdcUsername;
    private String sfdcPassword;
    private String sfdcQuery;

    /**
     * Creates a new SFDC connector
     * @param connectorBackend connector backend
     */
    protected SfdcConnector(ConnectorBackend connectorBackend) {
        super(connectorBackend);
    }

   /**
     * Creates a new SFDC connector
     * @param connectorBackend connector backend
     */
    public static SfdcConnector createConnector(ConnectorBackend connectorBackend) {
        return new SfdcConnector(connectorBackend);
    }

    /**
     * Executes the SFDC query
     * @param binding SFDC stub
     * @param sfdcQuery SFDC SOOL query
     * @return results as List of SObjects
     * @throws SfdcException in case of SFDC communication errors
     */
    private static List<SObject> executeQuery(SoapBindingStub binding, String sfdcQuery) throws SfdcException {
        l.debug("Executing SFDC query "+sfdcQuery);
        List<SObject> result = new ArrayList<SObject>();
        QueryOptions qo = new QueryOptions();
        qo.setBatchSize(500);
        binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(),
             "QueryOptions", qo);
        try {
            QueryResult qr = binding.query(sfdcQuery);
            do {
                SObject[] sObjects = qr.getRecords();
                result.addAll(Arrays.asList(sObjects));
                if(!qr.isDone()) {
                    qr = binding.queryMore(qr.getQueryLocator());
                }
            } while(!qr.isDone());
        }
        catch (ApiQueryFault ex) {
            l.error("Executing SFDC query failed",ex);
            throw new SfdcException("Failed to execute SFDC query.",ex);
        }
        catch (UnexpectedErrorFault e) {
            l.error("Executing SFDC query failed",e);
    	    throw new SfdcException("Failed to execute SFDC query.",e);
	    }
        catch (InvalidIdFault e) {
            l.error("Executing SFDC query failed",e);
	        throw new SfdcException("Failed to execute SFDC query.",e);
	    }
        catch (InvalidQueryLocatorFault e) {
            l.error("Executing SFDC query failed",e);
		    throw new SfdcException("Failed to execute SFDC query.",e);
	    }
        catch (RemoteException e) {
            l.error("Executing SFDC query failed",e);
		    throw new SfdcException("Failed to execute SFDC query.",e);
	    }
        l.debug("Finihed SFDC query execution.");
        return result;
    }

    /**
     * Executes the SFDC query, returns one row only. This is useful for metadata inspection purposes
     * @param binding SFDC stub
     * @param sfdcQuery SFDC SOOL query
     * @return results as List of SObjects
     * @throws SfdcException in case of SFDC communication errors
     */
    private static SObject executeQueryFirstRow(SoapBindingStub binding, String sfdcQuery) throws SfdcException {
        l.debug("Executing SFDC query "+sfdcQuery);
        List<SObject> result = new ArrayList<SObject>();
        QueryOptions qo = new QueryOptions();
        qo.setBatchSize(1);
        binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(),
             "QueryOptions", qo);
        try {
            QueryResult qr = binding.query(sfdcQuery);
            while (qr.getSize()>0) {
                SObject[] sObjects = qr.getRecords();
                result.addAll(Arrays.asList(sObjects));
            }
        }
        catch (ApiQueryFault ex) {
            l.error("Executing SFDC query failed",ex);
            throw new SfdcException("Failed to execute SFDC query.",ex);
        }
        catch (UnexpectedErrorFault e) {
            l.error("Executing SFDC query failed",e);
    	    throw new SfdcException("Failed to execute SFDC query.",e);
	    }
        catch (InvalidIdFault e) {
            l.error("Executing SFDC query failed",e);
	        throw new SfdcException("Failed to execute SFDC query.",e);
	    }
        catch (InvalidQueryLocatorFault e) {
            l.error("Executing SFDC query failed",e);
		    throw new SfdcException("Failed to execute SFDC query.",e);
	    }
        catch (RemoteException e) {
            l.error("Executing SFDC query failed",e);
		    throw new SfdcException("Failed to execute SFDC query.",e);
	    }
        l.debug("Finihed SFDC query execution.");
        return result.get(0);
    }

    /**
     * Retrieves the object's metadata
     * @param c SFDC stub
     * @param name SFDC object name
     * @return Map of fields
     * @throws RemoteException communication error
     */
    private static Map<String, Field> describeObject(SoapBindingStub c, String name) throws RemoteException {
        l.debug("Retrieving SFDC object "+name+" metadata.");
        Map<String,Field> result = new HashMap<String,Field>();
        DescribeSObjectResult describeSObjectResult = c.describeSObject(name);
        if (! (describeSObjectResult == null)) {
            Field[] fields = describeSObjectResult.getFields();
            if (fields != null) {
                for(Field field: fields) {
                    result.put(field.getName(), field);
                }
            }
        }
        l.debug("SFDC object \"+name+\" metadata retrieved.");
        return result;
    }


    /**
     * Saves a template of the config file
     * @throws IOException if there is a problem with writing the config file
     */
    public static void saveConfigTemplate(String name, String configFileName, String sfdcUsr, String sfdcPsw,
                                  String query)
            throws IOException {
        l.debug("Saving SFDC config template.");
        SourceSchema s = SourceSchema.createSchema(name);
        SoapBindingStub c = null;
        try {
            c = connect(sfdcUsr, sfdcPsw);
        } catch (ServiceException e) {
            throw new SfdcException("Connection to SFDC failed: "+e.getMessage());
        }
        SObject result = executeQueryFirstRow(c, query);
        if(result != null) {
            Map<String,Field> fields = describeObject(c, result.getType());
            for(MessageElement column : result.get_any()) {
                String nm = column.getName();
                String tp = getColumnType(fields, nm);
                if(tp.equals(SourceColumn.LDM_TYPE_DATE)) {
                    SourceColumn sc = new SourceColumn(StringUtil.formatShortName(nm), tp, nm, name);
                    sc.setFormat("yyyy-MM-dd");
                    s.addColumn(sc);
                }
                else {
                    SourceColumn sc = new SourceColumn(StringUtil.formatShortName(nm), tp, nm, name);
                    s.addColumn(sc);
                }
            }
        }
        else {
            l.error("The SFDC query hasn't returned any row.");
            throw new InvalidArgumentException("The SFDC query hasn't returned any row.");
        }
        s.writeConfig(new File(configFileName));
        l.debug("Saved SFDC config template.");
    }

    /**
     * Derives the LDM type from the SFDC type
     * @param fields SFDC object metadata
     * @param fieldName the field name
     * @return LDM type
     */
    private static String getColumnType(Map<String,Field> fields, String fieldName) {
        String type = SourceColumn.LDM_TYPE_ATTRIBUTE;
        Field f = fields.get(fieldName);
        if(f != null) {
            FieldType t = f.getType();
            if(t.getValue().equalsIgnoreCase("id"))
                type = SourceColumn.LDM_TYPE_CONNECTION_POINT;
            else if(t.getValue().equalsIgnoreCase("string"))
                type = SourceColumn.LDM_TYPE_ATTRIBUTE;
            else if(t.getValue().equalsIgnoreCase("currency"))
                type = SourceColumn.LDM_TYPE_FACT;
            else if(t.getValue().equalsIgnoreCase("boolean"))
                type = SourceColumn.LDM_TYPE_ATTRIBUTE;
            else if(t.getValue().equalsIgnoreCase("reference"))
                type = SourceColumn.LDM_TYPE_REFERENCE;
            else if(t.getValue().equalsIgnoreCase("date"))
                type = SourceColumn.LDM_TYPE_DATE;
            else if(t.getValue().equalsIgnoreCase("datetime"))
                type = SourceColumn.LDM_TYPE_DATE;
        }
        return type;
    }

    /**
     * {@inheritDoc}
     */
    public void extract() throws IOException {
        l.debug("Extracting SFDC data.");
        SoapBindingStub con = null;
        try {
            File dataFile = FileUtil.getTempFile();
            CSVWriter cw = new CSVWriter(new FileWriter(dataFile));
            SoapBindingStub c = connect(getSfdcUsername(), getSfdcPassword());
            List<SObject> result = null;
            try {
                result = executeQuery(c, getSfdcQuery());
            } catch (SfdcException e) {
                l.error("SFDC query execution failed.",e);
                throw new IOException("SFDC query execution failed: ",e);
            }
            if(result != null && result.size() > 0) {
                l.debug("Started retrieving SFDC data.");
                SObject firstRow = result.get(0);
                Map<String,Field> fields = describeObject(c, firstRow.getType());
                MessageElement[] frCols = firstRow.get_any();
                String[] colTypes = new String[frCols.length];
                for(int i=0; i< frCols. length; i++) {
                    String nm = frCols[i].getName();
                    colTypes[i] = getColumnType(fields, nm);
                }
                for( SObject row : result) {
                    MessageElement[] cols = row.get_any();
                    String[] vals = new String[cols.length];
                    for(int i=0; i<vals.length; i++) {
                        if(colTypes[i].equals(SourceColumn.LDM_TYPE_DATE))
                            vals[i] = cols[i].getValue().substring(0,10);
                        else
                            vals[i] = cols[i].getValue();
                    }
                    cw.writeNext(vals);
                }
                l.debug("Retrieved " + result.size() + " rows of SFDC data.");
            }
            cw.flush();
            cw.close();
            getConnectorBackend().extract(dataFile);
            FileUtil.recursiveDelete(dataFile);
        }
        catch (ServiceException e) {
            l.error("Error retrieving data from the SFDC source.", e);
            throw new IOException("Error retrieving data from the SFDC source.", e);
        }
        l.debug("Extracted SFDC data.");
    }

    /**
     * Connect the SFDC
     * @param usr SFDC username
     * @param psw SFDC pasword
     * @return SFDC stub
     * @throws ServiceException in case of connection issues
     */
    private static SoapBindingStub connect(String usr, String psw) throws ServiceException {
        SoapBindingStub binding = (SoapBindingStub) new SforceServiceLocator().getSoap();
        l.debug("Connecting to SFDC.");
        // Time out after a minute
        binding.setTimeout(60000);
        // Test operation
        LoginResult loginResult;
        try {
            loginResult = binding.login(usr, psw);
        }
        catch (LoginFault ex) {
            // The LoginFault derives from AxisFault
            ExceptionCode exCode = ex.getExceptionCode();
            if(exCode == ExceptionCode.FUNCTIONALITY_NOT_ENABLED) {
                l.error("Error logging into the SFDC. Functionality not enabled.", ex);
                throw new ServiceException("Error logging into the SFDC. Functionality not enabled.", ex);
            }
            else if(exCode == ExceptionCode.INVALID_CLIENT) {
                l.error("Error logging into the SFDC. Invalid client.", ex);
                throw new ServiceException("Error logging into the SFDC. Invalid client.", ex);
            }
            else if(exCode == ExceptionCode.INVALID_LOGIN) {
                l.error("Error logging into the SFDC. Invalid login.", ex);
                throw new ServiceException("Error logging into the SFDC. Invalid login.", ex);
            }
            else if(exCode == ExceptionCode.LOGIN_DURING_RESTRICTED_DOMAIN) {
                l.error("Error logging into the SFDC. Restricred domain.", ex);
                throw new ServiceException("Error logging into the SFDC. Restricred domain.", ex);
            }
            else if(exCode == ExceptionCode.LOGIN_DURING_RESTRICTED_TIME) {
                l.error("Error logging into the SFDC. Restricred during time.", ex);
                throw new ServiceException("Error logging into the SFDC. Restricred during time.", ex);
            }
            else if(exCode == ExceptionCode.ORG_LOCKED) {
                l.error("Error logging into the SFDC. Organization locked.", ex);
                throw new ServiceException("Error logging into the SFDC. Organization locked.", ex);
            }
            else if(exCode == ExceptionCode.PASSWORD_LOCKOUT) {
                l.error("Error logging into the SFDC. Password lock-out.", ex);
                throw new ServiceException("Error logging into the SFDC. Password lock-out.", ex);
            }
            else if(exCode == ExceptionCode.SERVER_UNAVAILABLE) {
                l.error("Error logging into the SFDC. Server not available.", ex);
                throw new ServiceException("Error logging into the SFDC. Server not available.", ex);
            }
            else if(exCode == ExceptionCode.TRIAL_EXPIRED) {
                l.error("Error logging into the SFDC. Trial expired.", ex);
                throw new ServiceException("Error logging into the SFDC. Trial expired.", ex);
            }
            else if(exCode == ExceptionCode.UNSUPPORTED_CLIENT) {
                l.error("Error logging into the SFDC. Unsupported client.", ex);
                throw new ServiceException("Error logging into the SFDC. Unsupported client.", ex);
            }
            else {
                l.error("Error logging into the SFDC.", ex);
                throw new ServiceException("Error logging into the SFDC.", ex);
            }
        } catch (Exception ex) {
            l.error("Error logging into the SFDC.", ex);
            throw new ServiceException("Error logging into the SFDC.", ex);
        }
        // Check if the password has expired
        if (loginResult.isPasswordExpired()) {
            l.error("An error has occurred. Your password has expired.");
        }
        /** Once the client application has logged in successfully, it will use
         *  the results of the login call to reset the endpoint of the service
         *  to the virtual server instance that is servicing your organization.
         *  To do this, the client application sets the ENDPOINT_ADDRESS_PROPERTY
         *  of the binding object using the URL returned from the LoginResult.
         */
        binding._setProperty(SoapBindingStub.ENDPOINT_ADDRESS_PROPERTY,
            loginResult.getServerUrl());
        /** The sample client application now has an instance of the SoapBindingStub
         *  that is pointing to the correct endpoint. Next, the sample client application
         *  sets a persistent SOAP header (to be included on all subsequent calls that
         *  are made with the SoapBindingStub) that contains the valid sessionId
         *  for our login credentials. To do this, the sample client application
         *  creates a new SessionHeader object and set its sessionId property to the
         *  sessionId property from the LoginResult object.
         */
        // Create a new session header object and add the session id
        // from the login return object
        SessionHeader sh = new SessionHeader();
        sh.setSessionId(loginResult.getSessionId());
        /** Next, the sample client application calls the setHeader method of the
         *  SoapBindingStub to add the header to all subsequent method calls. This
         *  header will persist until the SoapBindingStub is destroyed until the header
         *  is explicitly removed. The "SessionHeader" parameter is the name of the
         *  header to be added.
         */
        // set the session header for subsequent call authentication
        binding.setHeader(new SforceServiceLocator().getServiceName().getNamespaceURI(),
                          "SessionHeader", sh);
        l.debug("Connected to SFDC.");
        return binding;
    }

    /**
     * Connects to the SFDC
     * @return SFDC  stub
     * @throws ServiceException in case of connection issues
     */
    public SoapBindingStub connect() throws ServiceException {
        return connect(getSfdcUsername(), getSfdcPassword());
    }

    /**
     * SFDC username getter
     * @return SFDC username
     */
    public String getSfdcUsername() {
        return sfdcUsername;
    }

    /**
     * SFDC username setter
     * @param sfdcUsername SFDC username
     */
    public void setSfdcUsername(String sfdcUsername) {
        this.sfdcUsername = sfdcUsername;
    }

    /**
     * SFDC password getter
     * @return SFDC password
     */
    public String getSfdcPassword() {
        return sfdcPassword;
    }

    /**
     * SFDC password setter
     * @param sfdcPassword SFDC password
     */
    public void setSfdcPassword(String sfdcPassword) {
        this.sfdcPassword = sfdcPassword;
    }

    /**
     * SFDC query getter
     * @return SFDC query
     */
    public String getSfdcQuery() {
        return sfdcQuery;
    }

    /**
     * SFDC query setter
     * @param sfdcQuery SFDC query
     */
    public void setSfdcQuery(String sfdcQuery) {
        this.sfdcQuery = sfdcQuery;
    }

    /**
     * {@inheritDoc}
     */
    public boolean processCommand(Command c, CliParams cli, ProcessingContext ctx) throws ProcessingException {
        l.debug("Processing command "+c.getCommand());
        try {
            if(c.match("GenerateSfdcConfig")) {
                generateSfdcConfig(c, cli, ctx);
            }
            else if(c.match("LoadSfdc")) {
                loadSfdc(c, cli, ctx);
            }
            else {
                l.debug("No match passing the command "+c.getCommand()+" further.");
                return super.processCommand(c, cli, ctx);
            }
        }
        catch (IOException e) {
            throw new ProcessingException(e);
        }
        l.debug("Processed command "+c.getCommand());
        return true;
    }

    /**
     * Loads SFDC data command processor
     * @param c command
     * @param p command line arguments
     * @param ctx current processing context
     * @throws IOException in case of IO issues
     */
    private void loadSfdc(Command c, CliParams p, ProcessingContext ctx) throws IOException {
        String configFile = c.getParamMandatory("configFile");
        String usr = c.getParamMandatory( "username");
        String psw = c.getParamMandatory( "password");
        String q = c.getParamMandatory("query");
        File conf = FileUtil.getFile(configFile);
        initSchema(conf.getAbsolutePath());
        setSfdcUsername(usr);
        setSfdcPassword(psw);
        setSfdcQuery(q);
        // sets the current connector
        ctx.setConnector(this);
        setProjectId(ctx);
    }

    /**
     * Generates the SFDC config
     * @param c command
     * @param p command line arguments
     * @param ctx current processing context
     * @throws IOException in case of IO issues
     */
    private void generateSfdcConfig(Command c, CliParams p, ProcessingContext ctx) throws IOException {
        String configFile = c.getParamMandatory("configFile");
        String name = c.getParamMandatory("name");
        String usr = c.getParamMandatory( "username");
        String psw = c.getParamMandatory( "password");
        String query = c.getParamMandatory("query");
        File cf = new File(configFile);

        SfdcConnector.saveConfigTemplate(name, configFile, usr, psw, query);
    }

}