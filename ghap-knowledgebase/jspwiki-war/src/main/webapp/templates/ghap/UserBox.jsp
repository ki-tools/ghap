<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>

<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.apache.wiki.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
    WikiContext c = WikiContext.findContext(pageContext);
%>
<c:set var="redirect"><%= c.getEngine().encodeName(c.getName()) %></c:set>
<c:set var="username"><wiki:UserName /></c:set>
<c:set var="loginstatus"><wiki:Variable var='loginstatus'/></c:set>

<div class="cage top-nav pull-right userbox user-${loginstatus}">

    <span class="top-nav-text">
        <span class="icon-user"></span>
        <wiki:CheckRequestContext context='!prefs'>
            <wiki:Link jsp="UserPreferences.jsp">
                <wiki:Param name='redirect' value='${redirect}'/>
                <fmt:message key="actions.account" />
            </wiki:Link>
        </wiki:CheckRequestContext>
        <wiki:CheckRequestContext context='prefs'>
            <fmt:message key="actions.account" />
        </wiki:CheckRequestContext>


        &nbsp;&nbsp;|&nbsp;&nbsp;

        <wiki:UserCheck status="authenticated">
            <a href="<wiki:Link jsp='Logout.jsp' format='url' />"
               class="logout"
               data-modal=".modal">
                <fmt:message key="actions.logout"/>
                <div class="modal"><fmt:message key='actions.confirmlogout'/></div>
            </a>
        </wiki:UserCheck>
    </span>
</div>
