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
<%@page import="io.ghap.auth.Settings" %>

<%@page import="org.apache.wiki.auth.login.CookieAuthenticationLoginModule"%>
<%@page import="org.apache.wiki.WikiEngine" %>
<%@page import="org.apache.wiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="org.apache.wiki.WikiContext" %>

<%
  WikiEngine wiki = WikiEngine.getInstance(getServletConfig());

  wiki.getAuthenticationManager().logout( request );

  // Clear the user cookie
  CookieAssertionLoginModule.clearUserCookie( response );

  // Delete the login cookie
  CookieAuthenticationLoginModule.clearLoginCookie( wiki, request, response );

  // Redirect to the webroot
  // TODO: Should redirect to a "goodbye" -page?
  //response.sendRedirect(".");

%>
<html>
    <script type="text/javascript">
        var Settings = {
            FRONT_PAGE: '<%= wiki.getBaseURL() %>',
            OAUTH_URL: '<%= Settings.getInstance().getOauthUrl() %>'
        };

        ajax(Settings.OAUTH_URL + 'revoke', function(response) {
            document.location.href = Settings.OAUTH_URL +
                    'authorize?client_id=projectservice&response_type=token&redirect_uri=' + Settings.FRONT_PAGE + '?tokenViaQueryString';
        });



        //////////////////////////////////////

        function ajax(url, callback) {
            var xmlHttpRequest = getXMLHttpRequest();
            xmlHttpRequest.withCredentials = true;
            xmlHttpRequest.onreadystatechange = function() {
                if (xmlHttpRequest.readyState == 4) {
                    callback(xmlHttpRequest);
                }
            };
            xmlHttpRequest.open('get', url, true);
            //xmlHttpRequest.setRequestHeader("Authorization", "Bearer " + Settings.accessToken);
            xmlHttpRequest.send("");
        }

        /** AJAX Requests as per http://javapapers.com/ajax/getting-started-with-ajax-using-java/ **/
        /*
         * creates a new XMLHttpRequest object which is the backbone of AJAX,
         * or returns false if the browser doesn't support it
         */
        function getXMLHttpRequest() {
            var xmlHttpReq = false;
            // to create XMLHttpRequest object in non-Microsoft browsers
            if (window.XMLHttpRequest) {
                xmlHttpReq = new XMLHttpRequest();
            } else if (window.ActiveXObject) {
                try {
                    // to create XMLHttpRequest object in later versions
                    // of Internet Explorer
                    xmlHttpReq = new ActiveXObject("Msxml2.XMLHTTP");
                } catch (exp1) {
                    try {
                        // to create XMLHttpRequest object in older versions
                        // of Internet Explorer
                        xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
                    } catch (exp2) {
                        xmlHttpReq = false;
                    }
                }
            }
            return xmlHttpReq;
        }
    </script>
</html>