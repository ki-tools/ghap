/**
 * Created by Vlad on 21.05.2015.
 */

var Qs = require('qs');
var request = require('request');

var doAfter = null;

function GhapOAuth() {
	var
		oauth_Domain  = 'http://ghap-oauth-server-us-east-1-devtest.elasticbeanstalk.com/',
		oauth_LoginPath = 'oauth/authorize',
		client_id       = 'projectservice',
		response_type   = 'token',
		redirect_uri    = 'http://ghap.io',
		action_url      = 'j_spring_security_check';

	this.getAuthorize_Url = function() {
		return oauth_Domain + oauth_LoginPath
			+ '?client_id=' + client_id
			+'&response_type=' + response_type
			+'&redirect_uri=' + redirect_uri;
	};

	this.getAction_Url= function() {
		return oauth_Domain +  action_url;
	};

	this.access_token = null;

	this.header = {
		Name: 'Authorization',
		Value: 'Bearer '
	};

	return this;
}

GhapOAuth.prototype.login = function (username, password) {

	var start = new Date();
	var self = this;
	console.log('OAuth authorization started.');

	request({
		url: self.getAuthorize_Url(),
		method: 'GET',
		followRedirect: false
	}, function (error, response, body) {
		if (error) {
			console.error('Can not GET %s',self.getAuthorize_Url());
			console.info(error);
			return;
		}
		if (response.statusCode != 302) {
			console.log("Unexpected response status code %d on Get JSESSIONID request.", response.statusCode);
			return;
		}
		var cookie_str = getJSESSSIONID_cookie_str(response);
		if (cookie_str === null) {
			console.log("JSESSIONID not found in GET response.");
			return;
		}

		request({
			url: self.getAction_Url(),
			method: 'POST',
			json: false,
			headers: {
				'content-type': 'application/x-www-form-urlencoded',
				'cookie': cookie_str
			},
			body: Qs.stringify({
				j_username: username,
				j_password: password,
				'user-policy': 'on'
			})
		}, function (error, response, body) {
			if (response.statusCode != 302) {
				console.log("Unexpected response status code %d on Authorize request.", response.statusCode);
				return;
			}
			var cookie_str = getJSESSSIONID_cookie_str(response);
			if (cookie_str === null) {
				console.log("Second JSESSIONID not found in Authorize response.");
				return;
			}
			if (!response.headers.hasOwnProperty('location')) {
				console.log("Location header is not specified in Authorize response.");
				return;
			}
			var location = response.headers.location;

			request({
				url: location,
				method: 'GET',
				headers: {
					'cookie': cookie_str
				},
				followRedirect: false
			}, function (error, response, body) {
				if (response.statusCode != 302) {
					console.log("Unexpected response status code %d on Get token request.",response.statusCode);
					return;
				}
				if (!response.headers.hasOwnProperty('location')) {
					console.log("Location header is not specified in Get token response.");
					return;
				}
				location = response.headers.location;

				var rePattern = new RegExp("access_token=[A-Za-z0-9]+-[A-Za-z0-9-]+");
				var matches = location.match(rePattern);
				if (matches != null) {
					self.access_token = matches[0].substring("access_token=".length);
					self.header.Value += self.access_token;
					var exec_time_ms = new Date() - start;
					console.info("Access token '%s' for user '%s' is received within %dms", self.access_token, username, exec_time_ms);
					if (doAfter !== null) doAfter();
				} else
					console.log("'access_token' parameter not found in location header '%s'", location);
			})
		});

	});
	return this;
};

GhapOAuth.prototype.then = function(do_after) {
	if (this.access_token !== null)
		do_after();
	else
		doAfter = do_after;
};

module.exports = new GhapOAuth();

function getJSESSSIONID_cookie_str(response) {
	var jsessionid_cookie_str = null;
	if (response.headers.hasOwnProperty('set-cookie'))
		response.headers['set-cookie'].forEach(function (cookie_str) {
			if (cookie_str.indexOf('JSESSIONID') > -1)
				jsessionid_cookie_str = cookie_str;
		});
	return jsessionid_cookie_str;
}