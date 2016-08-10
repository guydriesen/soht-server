/******************************************************************************
 * $Source: /cvsroot/telnetoverhttp/servers/java/src/java/soht/server/java/SocketProxyAdminServlet.java,v $
 * $Revision: 1.3 $
 * $Author: edaugherty $
 * $Date: 2003/11/26 16:30:50 $
 ******************************************************************************
 * Copyright (c) 2003, Eric Daugherty (http://www.ericdaugherty.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Eric Daugherty nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 * *****************************************************************************
 * For current versions and more information, please visit:
 * http://www.ericdaugherty.com/dev/soht
 *
 * or contact the author at:
 * soht@ericdaugherty.com
 *****************************************************************************/

package soht.server;

import java.io.*;
import java.util.Properties;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.*;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * Provides functions to monitor and administer the SocketProxyServlet.
 * 
 * @author Eric Daugherty
 */
public class SocketProxyAdminServlet extends HttpServlet {

    //***************************************************************
    // Constants
    //***************************************************************

    //
    // URLs
    //

    public static final String ADMIN_HOME_PAGE = "index.jsp";
    public static final String LOGIN_PAGE = "login.jsp";

    //
    // Session Attribute Names
    //

    public static final String SESSION_VALID = "valid";
    public static final String SESSION_MESSAGE = "message";
    public static final String SESSION_ERROR_MESSAGE = "error.message";

    //
    // Actions
    //

    public static final String ACTION_LOGIN = "login";
    public static final String ACTION_LOGOUT = "logout";
    public static final String ACTION_REMOVE_CONNECTION = "remove";
    public static final String ACTION_SET_LOGIN_REQUIRED = "setloginrequired";
    public static final String ACTION_CHANGE_PASSWORD_FOR_SYSTEM = "changepasswordforsystem";
    public static final String ACTION_ADD_USER = "adduser";
    public static final String ACTION_DELETE_USER = "deleteuser";
    public static final String ACTION_CHANGE_PASSWORD_FOR_USER = "changepasswordforuser";
    public static final String ACTION_CHANGE_PASSWORD_BY_USER = "changepasswordbyuser";
    public static final String ACTION_CHANGE_BLOCK_SIZE = "changeblocksize";

    //
    // Property File Keys
    //

    public static final String PROP_BLOCK_SIZE = "system.blocksize";

    public static final int PROP_BLOCK_SIZE_DEFAULT = 16;

    //***************************************************************
    // Variables
    //***************************************************************

    /** Handles the logging of messages */
    private Logger log = Logger.getLogger( this.getClass() );

    /** Allows other servlets to the system properties */
    private static Properties properties;

    /** The location of the system properties.  Used to save any changes */
    private static File propertiesFile;

    /** The block size (in kilobytes) of the size of the response to write */
    private static int blockSize;

    //***************************************************************
    // HttpServlet Methods
    //***************************************************************

    /**
     * Called when the application is deployed.  Loads the properties file
     *
     * @param servletConfig
     * @throws javax.servlet.ServletException
     */
    public void init( ServletConfig servletConfig ) throws ServletException
    {
        super.init( servletConfig );

        // Load the properties.
        initializeProperties();

        // Initialize the logging subsystem.
        initializeLogger();

        log.info( "SOHT Initialized using properties file: " + propertiesFile.getAbsolutePath() );
    }

    /**
     * There should be no 'GET' requests to this servlet, so redirect the user to the home page.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        response.sendRedirect( ADMIN_HOME_PAGE );
    }

    /**
     * Performs specific actions on the TelnetProxy Servlet.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession();
        Boolean isValid = (Boolean) session.getAttribute( SESSION_VALID );
        String action = request.getParameter( "action" ).trim();
        String resultPage = LOGIN_PAGE;

        try
        {
            // Handle any actions that do not require the user to be logged in.
            String insecureResultPage = handleInsecureActions( action, request, session);

            // If the result of the handleInsecureActions is not null,
            // then an action was handled and we can go ahead and redirect
            // the user.
            if( insecureResultPage != null )
            {
                resultPage = insecureResultPage;
                session.setAttribute( SESSION_MESSAGE, "Action Successful.");
            }
            // If the session is not valid, we do not want to process
            // anything else.
            else if( isValid == null || !isValid.booleanValue() )
            {
                resultPage = LOGIN_PAGE;
            }
            else
            {
                // Change the default result page.
                resultPage = ADMIN_HOME_PAGE;

                String secureResultPage = handleSecureActions( action, request );
                if( secureResultPage != null )
                {
                    resultPage = secureResultPage;
                    session.setAttribute( SESSION_MESSAGE, "Action Successful.");
                }
                else
                {
                    resultPage = ADMIN_HOME_PAGE;
                    session.setAttribute( SESSION_MESSAGE, "No Action Performed.");
                }
            }

        }
        catch( UIException uiException )
        {
            session.setAttribute( SESSION_ERROR_MESSAGE, uiException.getMessage() );
        }
        finally
        {
            response.sendRedirect( resultPage );
        }
    }

    //***************************************************************
    // Public Static Helper Methods
    //***************************************************************

    public static List getUsers()
    {
        return User.loadUsers( properties );
    }

    public static User getUser( String userName )
    {
        return User.getUser( userName, properties );
    }

    public static boolean isPasswordRequired()
    {
        String passwordRequired = properties.getProperty( "system.loginrequired" );
        return ( passwordRequired != null && passwordRequired.equalsIgnoreCase( "true" ) );
    }

    /**
     * Checks to see if the specified session is currently considered
     * secure.
     *
     * @param session the session to check
     * @return true if the session is secure
     */
    public static boolean isSessionValid( HttpSession session )
    {
        Boolean isValid = (Boolean) session.getAttribute( SESSION_VALID );
        return ( isValid != null && isValid.booleanValue() );
    }

    /**
     * The max size (in kilobytes) of the response to a READ request.
     *
     * @return response size in kilobytes.
     */
    public static int getBlockSize() {
        return blockSize;
    }

    //***************************************************************
    // Private Handle Methods
    //***************************************************************

    /**
     * Checks for any actions that can be performed while
     * not authenticated.  Returns the uri of the page
     * if an action was performed, or null if no action
     * was performed.
     *
     * @return page uri, or null.
     */
    private String handleInsecureActions( String action, HttpServletRequest request, HttpSession session ) throws UIException
    {
        String resultPage = null;

        if( action.equalsIgnoreCase( ACTION_LOGIN ) )
        {
            resultPage = handleLogin( request, session );
        }
        else if( action.equals( ACTION_LOGOUT ) )
        {
            resultPage = handleLogout( session );
        }
        else if( action.equals( ACTION_CHANGE_PASSWORD_BY_USER ) )
        {
            resultPage = handleChangePasswordByUser( request );
        }
        return resultPage;
    }

    /**
     * Checks for any actions that can be performed while
     * authenticated.  Returns the uri of the page to redirect
     * to.
     *
     * @param action the action to perform
     * @param request request
     * @return the page to redirect the user to.
     * @throws UIException thrown if an error should be displayed to the user.
     */
    private String handleSecureActions( String action, HttpServletRequest request ) throws UIException
    {
        String resultPage = null;

        if( action.equals( ACTION_REMOVE_CONNECTION ) )
        {
            resultPage = handleRemoveConnection( request );
        }
        else if( action.equals( ACTION_SET_LOGIN_REQUIRED ) )
        {
            resultPage = handleSetLoginRequired( request );
        }
        else if( action.equals( ACTION_CHANGE_PASSWORD_FOR_SYSTEM ) )
        {
            resultPage = handleChangePasswordForSystem( request );
        }
        else if( action.equals( ACTION_ADD_USER ) )
        {
            resultPage = handleAddUser( request );
        }
        else if( action.equals( ACTION_DELETE_USER) )
        {
            resultPage = handleDeleteUser( request );
        }
        else if( action.equals( ACTION_CHANGE_PASSWORD_FOR_USER ) )
        {
            resultPage = handleChangePasswordForUser( request );
        }
        else if( action.equals( ACTION_CHANGE_BLOCK_SIZE ) )
        {
            resultPage = handleChangeBlockSize( request );
        }
        return resultPage;
    }

    /**
     * Handles the ACTION_LOGIN request.
     *
     * @param request request
     * @param session session
     * @return the page to redirect the user to.
     * @throws UIException thrown if an error should be displayed to the user.
     */
    private String handleLogin( HttpServletRequest request, HttpSession session ) throws UIException
    {
        String password = checkNull( request.getParameter( "password" ) );

        String encryptedPassword = User.encryptPassword( password );
        if( password == null || !encryptedPassword.equals( properties.getProperty( "system.password" ) ) )
        {
            throw new UIException( "Invalid Password" );
        }

        session.setAttribute( SESSION_VALID, new Boolean( true ) );
        return ADMIN_HOME_PAGE;
    }

    /**
     * Handles the ACTION_LOGOUT request
     *
     * @param session session
     * @return the page to redirect the user to.
     */
    private String handleLogout( HttpSession session )
    {
        session.removeAttribute( SESSION_VALID );
        return LOGIN_PAGE;
    }

    /**
     * Handles ACTION_CHANGE_PASSWORD_BY_USER request.
     * <p/>
     * This action can be performed without logging in as long as
     * they specify the corerct current password for the user.
     *
     * @param request request
     * @return the page to redirect the user to.
     * @throws UIException thrown if an error should be displayed to the user.
     */
    private String handleChangePasswordByUser( HttpServletRequest request ) throws UIException
    {

        String userName = checkNull( request.getParameter( "username" ) );
        String currentPassword = checkNull( request.getParameter( "currentpassword" ) );
        String password = checkNull( request.getParameter( "password" ) );
        String password2 = checkNull( request.getParameter( "password2" ) );


        if( properties.getProperty( "user." + userName ) == null )
        {
            throw new UIException( "User does not exist!" );
        }

        User user = User.getUser( userName, properties );

        if( !user.getPassword().equals( User.encryptPassword( currentPassword ) ) )
        {
            throw new UIException( "Current password is incorrect!" );
        }

        try
        {
            user.setPassword( password, password2 );
            User.setUser( user, properties );
            saveProperties();
        }
        catch( IOException ioException )
        {
            log.error( "Error saving properties after changeuserpassword action.", ioException );
            throw new UIException( "Unable to save properties.  Change not persisted!" );
        }

        return LOGIN_PAGE;
    }

    /**
     * Handle ACTION_REMOVE_CONNECTION action.
     *
     * @param request request
     * @return the page to redirect the user to.
     * @throws UIException thrown if an error should be displayed to the user.
     */
    private String handleRemoveConnection( HttpServletRequest request ) throws UIException
    {
        String idString = checkNull( request.getParameter( "id" ) );
        try
        {
            long id = Long.parseLong( idString );
            SocketProxyServlet.getConnectionManager().removeConnection( id );
        }
        catch( NumberFormatException numberFormatException )
        {
            throw new UIException( "Invalid connection ID!" );
        }

        return ADMIN_HOME_PAGE;
    }

    /**
     * Handle ACTION_SET_LOGIN_REQUIRED action.
     *
     * @param request request
     * @return the page to redirect the user to.
     * @throws UIException thrown if an error should be displayed to the user.
     */
    private String handleSetLoginRequired( HttpServletRequest request ) throws UIException
    {
        String requiresPassoword = checkNull( request.getParameter( "requirepassword" ) );

        if( !requiresPassoword.equalsIgnoreCase( "true" ) && !requiresPassoword.equalsIgnoreCase( "false") )
        {
            throw new UIException( "Invalid value for requirespassword!" );
        }

        try
        {
            properties.setProperty( "system.loginrequired", requiresPassoword.toLowerCase() );
            saveProperties();
        }
        catch( IOException ioException )
        {
            log.error( "Error saving properties after requirepassword action.", ioException );
            throw new UIException( "Unable to save properties.  Change not persisted!" );
        }

        return ADMIN_HOME_PAGE;
    }

    /**
     * Handle ACTION_CHANGE_PASSWORD_FOR_SYSTEM action.
     *
     * @param request request
     * @return the page to redirect the user to.
     * @throws UIException thrown if an error should be displayed to the user.
     */
    private String handleChangePasswordForSystem( HttpServletRequest request ) throws UIException
    {
        String password = checkNull( request.getParameter( "password" ) );
        String password2 = checkNull( request.getParameter( "password2" ) );

        try
        {
            User.validatePasswords( password, password2 );
            String encryptedPassword = User.encryptPassword( password );
            if( encryptedPassword == null )
            {
                throw new UIException( "Error encrypting password!" );
            }
            properties.setProperty( "system.password", encryptedPassword );
            saveProperties();
        }
        catch( IOException ioException )
        {
            log.error( "Error saving properties after changepasswordforsystem action.", ioException );
            throw new UIException( "Unable to save properties.  Change not persisted!" );
        }

        return ADMIN_HOME_PAGE;
    }

    /**
     * Handle ACTION_ADD_USER action.
     *
     * @param request request
     * @return the page to redirect the user to.
     * @throws UIException thrown if an error should be displayed to the user.
     */
    private String handleAddUser( HttpServletRequest request ) throws UIException
    {
        String userName = checkNull( request.getParameter( "username" ) );
        String password = checkNull( request.getParameter( "password" ) );
        String password2 = checkNull( request.getParameter( "password2" ) );

        if( properties.getProperty( "user." + userName ) != null )
        {
            throw new UIException( "Username already exists!" );
        }

        try
        {
            User user = new User( userName, password, password2 );
            User.setUser( user, properties );
            saveProperties();
        }
        catch( IOException ioException )
        {
            log.error( "Error saving properties after deleteuser action.", ioException );
            throw new UIException( "Unable to save properties.  Change not persisted!" );
        }

        return ADMIN_HOME_PAGE;
    }

    /**
     * Handle ACTION_DELETE_USER action.
     *
     * @param request request
     * @return the page to redirect the user to.
     * @throws UIException thrown if an error should be displayed to the user.
     */
    private String handleDeleteUser( HttpServletRequest request ) throws UIException
    {
        String userName = checkNull( request.getParameter( "username" ) );

        if( properties.getProperty( "user." + userName ) == null )
        {
            throw new UIException( "User does not exist!");
        }

        try
        {
            properties.remove( "user." + userName );
            saveProperties();
        }
        catch( IOException ioException )
        {
            log.error( "Error saving properties after deleteuser action.", ioException );
            throw new UIException( "Unable to save properties.  Change not persisted!" );
        }

        return ADMIN_HOME_PAGE;
    }

    /**
     * Handle ACTION_CHANGE_PASSWORD_FOR_USER action.
     *
     * @param request request
     * @return the page to redirect the user to.
     * @throws UIException thrown if an error should be displayed to the user.
     */
    private String handleChangePasswordForUser( HttpServletRequest request ) throws UIException
    {
        String userName = checkNull( request.getParameter( "username" ) );
        String password = checkNull( request.getParameter( "password" ) );
        String password2 = checkNull( request.getParameter( "password2" ) );

        if( properties.getProperty( "user." + userName ) == null )
        {
            throw new UIException( "User does not exist!" );
        }

        try
        {
            User user = User.getUser( userName, properties );
            user.setPassword( password, password2 );
            User.setUser( user, properties );
            saveProperties();
        }
        catch( IOException ioException )
        {
            log.error( "Error saving properties after changeuserpassword action.", ioException );
            throw new UIException( "Unable to save properties.  Change not persisted!" );
        }

        return ADMIN_HOME_PAGE;
    }

    private String handleChangeBlockSize( HttpServletRequest request ) throws UIException
    {
        String blockSizeString = checkNull( request.getParameter( "blocksize" ) );

        try
        {
            blockSize = Integer.parseInt( blockSizeString );
        }
        catch( NumberFormatException numberFormatException )
        {
            throw new UIException( "Unable to parse input." );
        }

        try
        {
            properties.setProperty( PROP_BLOCK_SIZE, String.valueOf( blockSize ) );
            saveProperties();
        }
        catch( IOException ioException )
        {
            log.error( "Error saving properties after changeblocksize action.", ioException );
            throw new UIException( "Unable to save properties.  Change not persisted!" );
        }

        return ADMIN_HOME_PAGE;
    }

    //***************************************************************
    // Private Util Methods
    //***************************************************************

    /**
     * Loads the soht properties file.  If the user specified the system
     * parameter soht.properties, the file will be loaded from there.
     * Otherwise the file will be loaded from the default location.  If it
     * does not exist in the default location, a default file will be copied
     * to the default location.
     */
    private void initializeProperties() throws ServletException
    {
        properties = new Properties();

        String fileName = System.getProperty( "soht.properties" );
        if( fileName != null && !fileName.equals( "" ) )
        {
            // The user specified file path for the properties file.  Load it from there.
            propertiesFile = new File( fileName );
            if( propertiesFile.exists() && propertiesFile.isFile() )
            {
                // Load the properties file from the specified location.
                try
                {
                    loadProperties();
                }
                catch( IOException ioException )
                {
                    // If we got here, the file exists and is a file, but there
                    // was some error loading it.  Not much we can do, so just
                    // error out.  This should never happen.
                    throw new ServletException( "Unable to load the properties file from the specified location: " + propertiesFile.getAbsolutePath() + " due to an IOException: " + ioException );
                }
            }
            else
            {
                throw new ServletException( "The specified properties file: " + propertiesFile.getAbsolutePath() + " does not exist." );
            }
        }
        else
        {
            // If the soht.properties location was not specified as a
            // system property, attempt to load it from the default
            // location.

            System.out.println( "The system property \"soht.properties\" was not specified.  Using the default location." );

            propertiesFile = new File( "soht.properties" );
            if( propertiesFile.exists() && propertiesFile.isFile() )
            {
                // Load the properties file from the default location.
                try
                {
                    loadProperties();
                }
                catch( IOException ioException )
                {
                    // If we got here, the file exists and is a file, but there
                    // was some error loading it.  Not much we can do, so just
                    // error out.  This should never happen.
                    throw new ServletException( "Unable to load the properties file from the default location: " + propertiesFile.getAbsolutePath() + " due to an IOException: " + ioException );
                }
            }
            else
            {
                // The default properties file does not exist, so attempt
                // to copy the properties file from the war to the default
                // file location.

                InputStream propertiesStream = getServletContext().getResourceAsStream("/WEB-INF/soht.properties");
                if( propertiesStream != null )
                {
                    try
                    {
                        // Load from the WAR
                        properties.load( propertiesStream );
                        // Save to the default location.
                        saveProperties();
                    }
                    catch( IOException ioException )
                    {
                        throw new ServletException( "Error copying the properties file from the WAR to the default location: " + propertiesFile.getAbsolutePath() + " due to an IOException: " + ioException );
                    }
                }
                else
                {
                    throw new ServletException( "Unable to load soht.properties from WAR.  SOHT will not function correctly!" );
                }
            }
        }
    }

    /**
     * Loads the properties from the propertiesFile
     * location.
     */
    private void loadProperties() throws IOException
    {
        InputStream propertiesStream = null;
        try
        {
            propertiesStream = new FileInputStream( propertiesFile );
            properties.load( propertiesStream );

            String blockSizeString = properties.getProperty( PROP_BLOCK_SIZE, String.valueOf( PROP_BLOCK_SIZE_DEFAULT ) );
            try
            {
                blockSize = Integer.parseInt( blockSizeString );
            }
            catch( NumberFormatException numberFormatException )
            {
                blockSize = PROP_BLOCK_SIZE_DEFAULT;
            }
        }
        finally
        {
            if( propertiesStream != null )
            {
                propertiesStream.close();
            }
        }
    }

    /**
     * Persists the properties to disk.  This should be called
     * after any changes to the configuration made by the user.
     */
    private void saveProperties() throws IOException
    {

        OutputStream propertiesStream = null;
        try
        {
            propertiesStream = new FileOutputStream( propertiesFile );
            properties.store( propertiesStream, "" );
        }
        finally
        {
            if( propertiesStream != null )
            {
                propertiesStream.close();
            }
        }
    }

    /**
     * Initializes the logging subsystem.  The properties file is already
     * assumed to be valid.
     */
    private void initializeLogger()
    {
        // The disablelog4j property allows the log4j initialization to be ignored.
        String disableLog4j = properties.getProperty( "disablelog4j" );
        if( disableLog4j == null || !Boolean.valueOf( disableLog4j ).booleanValue() )
        {
            PropertyConfigurator.configureAndWatch( propertiesFile.getAbsolutePath() );
            log.info( "Logger initialized" );
        }
        else
        {
            log.warn( "SOHT Log4j initialization disabled.");
        }
    }

    /**
     * Throws a UIException if the parameter is null.
     *
     * @param string paramter to check for null.
     * @return the parameter
     * @throws UIException thrown if the parameter is null.
     */
    private String checkNull( String string ) throws UIException
    {
        if( string == null )
        {
            throw new UIException( "Invalid null parameter." );
        }
        return string;
    }
}

//EOF
