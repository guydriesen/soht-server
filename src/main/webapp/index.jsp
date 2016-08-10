<%@ taglib uri="http://jakarta.apache.org/taglibs/i18n-1.0" prefix="i18n" %>
<%@ page contentType="text/html; charset=utf-8" %>
<i18n:bundle baseName="i18n"
             localeRef="userLocale"
             scope="request"
             changeResponseLocale="true"/>
<%@ page import="java.util.Map,
                 java.util.Iterator,
                 java.util.List,
                 soht.server.*"%><html>
<head>
    <title><i18n:message key="global.title"/></title>
    <link rel=StyleSheet href='soht.css'>
</head>

<%
    // Validate user is logged in.
    if( !SocketProxyAdminServlet.isSessionValid( session ) )
    {
        response.sendRedirect( SocketProxyAdminServlet.LOGIN_PAGE );
    }

    ConnectionManager connectionManager = SocketProxyServlet.getConnectionManager();
%>

<body>
    <h1><i18n:message key="global.title"/></h1>
<div>
    <jsp:include page="display-messages.jsp" />
</div>
<div>    
    <form method='POST' action='admin'>
        <input type='hidden' name='action' value='<%= SocketProxyAdminServlet.ACTION_LOGOUT %>' />
        <input value='<i18n:message key="logout"/>' type='submit' />
    </form>
</div>
    <div id="CurrentStatus">
    	<h2><i18n:message key="currentStatus"/></h2>
        <ul>
            <li><i18n:message key="totalConnections"/> : <%= connectionManager.getConnectionCount() %></li>
            <li><i18n:message key="readerThreads"/> : <%= SocketProxyServlet.getReaderCount() %></li>
            <li><i18n:message key="writerThreads"/> : <%= SocketProxyServlet.getWriterCount() %></li>
        </ul>
    </div>

    <div id="CurrentConnections">
    	<h2><i18n:message key="currentConnections"/></h2>
    <%
        Iterator connections = SocketProxyServlet.getConnectionManager().getConnections();
        ConnectionInfo info;
        while( connections.hasNext() ) {

            info = (ConnectionInfo) connections.next();
    %>
        <form method='POST' action='admin'>
            <input type='hidden' name='action' value='<%= SocketProxyAdminServlet.ACTION_REMOVE_CONNECTION %>' />
            <input type='hidden' name='id' value='<%= info.getConnectionId() %>' />
            ID: <%= info.getConnectionId() %> <%= info.toString() %> );
            <input value='Close' type='submit' />
        </form>
    <%
        }
    %>

    </div>

    <div id="RequireLogin">
        <h2><i18n:message key="requirePassword"/></h2>
        <p>
	    <i18n:message key="requirePassword.text"/>
        </p>
        <form method='POST' action='admin'>
            <input type='hidden' name='action' value='<%= SocketProxyAdminServlet.ACTION_SET_LOGIN_REQUIRED %>' />
            <%
                String trueSelected = "";
                String falseSelected ="";
                if( SocketProxyAdminServlet.isPasswordRequired() )
                {
                    trueSelected = "selected";
                }
                else
                {
                    falseSelected = "selected";
                }
            %>
            <select name="requirepassword">
              <option value="true" <%= trueSelected %> ><i18n:message key="requirePassword"/></option>
              <option value="false" <%= falseSelected%> ><i18n:message key="noPasswordRequired"/></option>
            </select>
            <input value='<i18n:message key="save"/>' type='submit' />
        </form>
    </div>

    <div id="SystemPassword">
        <h2><i18n:message key="systemPassword"/></h2>
        <i18n:message key="systemPassword.text"/> :
        <form method='POST' action='admin'>
            <input type='hidden' name='action' value='<%= SocketProxyAdminServlet.ACTION_CHANGE_PASSWORD_FOR_SYSTEM %>' />
            <i18n:message key="password"/> :
            <input type='password' name='password' />
            <br/>
            <i18n:message key="verifyPassword"/>:
            <input type='password' name='password2' />
            <br/>
            <input value='<i18n:message key="changePassword"/>' type='submit' />
        </form>
    </div>

    <div id="Users">
        <h2><i18n:message key="userMaintenance"/></h2>
        <h3><i18n:message key="addUser"/></h3>
        <form method='POST' action='admin'>
            <input type='hidden' name='action' value='<%= SocketProxyAdminServlet.ACTION_ADD_USER %>' />
            <i18n:message key="username"/> :
            <input type='text' name='username' />
            <br/>
            <i18n:message key="password"/> :
            <input type='password' name='password' />
            <br/>
            <i18n:message key="verifyPassword"/> :
            <input type='password' name='password2' />
            <br/>
            <input value='<i18n:message key="addUser"/>' type='submit' />
        </form>
        <h3><i18n:message key="updateRemoveUsers"/></h3>
        <p/>
        <%
            Iterator users = SocketProxyAdminServlet.getUsers().iterator();
            User user;
            while( users.hasNext() )
            {
                user = (User) users.next();
        %>
                <%= user.getUserName() %>
                <form method='POST' action='admin'>
                    <input type='hidden' name='action' value='<%= SocketProxyAdminServlet.ACTION_DELETE_USER %>' />
                    <input type='hidden' name='username' value='<%= user.getUserName() %>' />
                    <input type='submit' value='<i18n:message key="deleteUser"/>' />
                </form>

                <form method='POST' action='admin'>
                    <input type='hidden' name='action' value='<%= SocketProxyAdminServlet.ACTION_CHANGE_PASSWORD_FOR_USER %>' />
                    <input type='hidden' name='username' value='<%= user.getUserName() %>' />
                    <i18n:message key="password"/> :
                    <input type='password' name='password' />
                    <br/>
                    <i18n:message key="verifyPassword"/> :
                    <input type='password' name='password2' />
                    <br/>
                    <input value='<i18n:message key="changePassword"/>' type='submit' />
                </form>
        <%
            }
        %>
    </div>

</body>

</html>