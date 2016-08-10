<%@ taglib uri="http://jakarta.apache.org/taglibs/i18n-1.0" prefix="i18n" %>
<%@ page contentType="text/html; charset=utf-8" %>
<i18n:bundle baseName="i18n"
             localeRef="userLocale"
             scope="request"
             changeResponseLocale="true"/>
<%@ page import="soht.server.SocketProxyAdminServlet"%><html>
<head>
    <title><i18n:message key="global.title"/></title>
    <link rel=StyleSheet href='soht.css'>
</head>

<body>
	<h1><i18n:message key="global.title"/></h1>

    <jsp:include page="display-messages.jsp" />

    <div id="Login">
    	<h2><i18n:message key="adminLogin"/></h2>    
        <form method='POST' action='admin'>
            <input type='hidden' name='action' value='login' />
            <input type='password' name='password' />
            <input value='<i18n:message key="login"/>' type='submit' />
        </form>
    </div>

    <div id="ChangePassword">
    	<h2><i18n:message key="changePassword"/></h2>    
        <form method='POST' action='admin'>
            <input type='hidden' name='action' value='<%= SocketProxyAdminServlet.ACTION_CHANGE_PASSWORD_BY_USER %>' />
            <i18n:message key="username"/> :
            <input type='text' name='username' />
            <br/>
            <i18n:message key="currentPassword"/> :
            <input type='password' name='currentpassword' />
            <br/>
            <i18n:message key="password"/> :
            <input type='password' name='password' />
            <br/>
            <i18n:message key="verifyPassword"/> :
            <input type='password' name='password2' />
            <br/>
            <input value='<i18n:message key="changePassword"/>' type='submit' />
        </form>
    </div>
</body>

</html>