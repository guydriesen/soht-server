<%@ page import="soht.server.SocketProxyAdminServlet"%>
<%
    String errorMessage = (String) session.getAttribute( SocketProxyAdminServlet.SESSION_ERROR_MESSAGE );
    if( errorMessage != null )
    {
        session.removeAttribute( SocketProxyAdminServlet.SESSION_ERROR_MESSAGE );
%>
        <div id="Error">
            <%= errorMessage %>
        </div>
<%
    }

    String message = (String) session.getAttribute( SocketProxyAdminServlet.SESSION_MESSAGE );
    if( message != null )
    {
        session.removeAttribute( SocketProxyAdminServlet.SESSION_MESSAGE );
%>
        <div id="Message">
            <%= message %>
        </div>
<%
    }
%>
