
<%@ page import="io.ghap.oauth.AccessToken" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'accessToken.label', default: 'AccessToken')}" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
		<a href="#list-accessToken" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
		<div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
			</ul>
		</div>
		<div id="list-accessToken" class="content scaffold-list" role="main">
			<h1><g:message code="default.list.label" args="[entityName]" /></h1>
			<g:if test="${flash.message}">
				<div class="message" role="status">${flash.message}</div>
			</g:if>
			<table>
			<thead>
					<tr>
					
						<g:sortableColumn property="username" title="${message(code: 'accessToken.username.label', default: 'Username')}" />
					
						<g:sortableColumn property="clientId" title="${message(code: 'accessToken.clientId.label', default: 'Client Id')}" />
					
						<g:sortableColumn property="value" title="${message(code: 'accessToken.value.label', default: 'Value')}" />
					
						<g:sortableColumn property="tokenType" title="${message(code: 'accessToken.tokenType.label', default: 'Token Type')}" />
					
						<g:sortableColumn property="expiration" title="${message(code: 'accessToken.expiration.label', default: 'Expiration')}" />

						<g:sortableColumn property="lastUsed" title="${message(code: 'accessToken.expiration.label', default: 'Last Used')}" />

						<th>Expired on</th>
						<th>Scopes</th>
						<th>Actions</th>
					</tr>
				</thead>
				<tbody>
				<g:each in="${accessTokenInstanceList}" status="i" var="accessTokenInstance">
					<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					
						<td>${fieldValue(bean: accessTokenInstance, field: "username")}</td>
					
						<td>${fieldValue(bean: accessTokenInstance, field: "clientId")}</td>
					
						<td>${fieldValue(bean: accessTokenInstance, field: "value")}</td>
					
						<td>${fieldValue(bean: accessTokenInstance, field: "tokenType")}</td>
					
						<td><g:formatDate date="${accessTokenInstance.expiration}" /></td>
						<td><g:formatDate date="${accessTokenInstance.lastUsed}" /></td>
						<td><g:formatDate date="${io.ghap.oauth.GhapTokenStoreService.getMinDate(accessTokenInstance)}" /></td>

						<td>
							<g:each in="${accessTokenInstance?.scope}" var="s">
								${s};&nbsp;
							</g:each>
						</td>
						<td><g:link action="deleteById" id="${accessTokenInstance?.id}">Delete</g:link> </td>
					</tr>
				</g:each>
				</tbody>
			</table>
			<div class="pagination">
				<g:paginate total="${accessTokenInstanceCount ?: 0}" />
			</div>
		</div>
	</body>
</html>
