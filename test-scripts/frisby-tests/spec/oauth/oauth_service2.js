var my = require('./../Common/ghap-lib');
var cfg = require('./../Common/ghap-config');
var frisby  = require('frisby');

var debugLog = false;

function OAuth(){
	var
		oauth_Domain    = cfg.oauthDomain,
	  oauth_LoginPath = '/oauth/authorize',
		client_id       = 'projectservice',
		response_type   = 'token',
		redirect_uri    = 'http://ghap.io',
		action_url      = '/j_spring_security_check',
		revoke_url      = '/oauth/revoke';

	this.getAuthorize_Url = function() {
		return oauth_Domain + oauth_LoginPath
			+ '?client_id=' + client_id
			+'&response_type=' + response_type
			+'&redirect_uri=' + redirect_uri;
	};

	this.getAction_Url= function() {
		return oauth_Domain +  action_url;
	};

	this.getRevoke_Url= function() {
		return oauth_Domain +  revoke_url;
	};

	this.access_token = null;
	this.is_access_token_accepted = false;
	this.doAfter = null;
	this.username = '';
	this.jsession_cookie = '';

	/**
	 *
	 * @type {GhapAuthHeader}
	 */
	this.header = {
		Name: 'Authorization',
		Value: 'Bearer '
	};

	return this;
}

/**
 *
 * @param {string} username
 * @param {string} password
 * @returns {OAuth}
 */
OAuth.prototype.login = function(username, password){

	console.log("Start oAuth authorization for user '%s'.",username);
	var self = this;
	// reset parameters for reuse
	self.access_token = null;
	self.is_access_token_accepted = false;
	self.header.Value = 'Bearer ';
	self.username = username;
	var start = new Date();

	frisby.create(my.getStepNumStr() + 'OAuth Get JSESSIONID')
		.get(self.getAuthorize_Url(),
		{followRedirect: false})
		.expectStatus(302)
		.after(function (err, response, body) {
			if (debugLog){
				console.log("GET request '%s'",self.getAuthorize_Url());
				console.log('GET response status code '+response.statusCode);
				console.log('Response headers:');
				console.log(response.headers);
			}
			if (response.statusCode != 302) {
				console.log("Unexpected response status code %d on Get JSESSIONID request.", response.statusCode);
				self.is_access_token_accepted = true;
				return
			}
			expect(response.headers['set-cookie']).toBeDefined();
			var cookie_str = getJSESSSIONID_cookie_str(response);
			expect(cookie_str).not.toBeNull();
			if (cookie_str === null) {
				console.log("JSESSIONID not found in GET response.");
				self.is_access_token_accepted = true;
				return
			}
			if (debugLog) console.log('Step 1 success ---------------------------------------');
			frisby.create(my.getStepNumStr() + 'OAuth Authorize')
				.post(self.getAction_Url(), {
					j_username: username,
					j_password: password,
					'user-policy': 'on'
				})
				.addHeader('cookie', cookie_str)
				.expectStatus(302)
				.after(function (err, response, body) {

					if (debugLog) {
						console.log("POST request '%s'", self.getAction_Url());
						var request = response.request;
						console.log('Request headers:');
						console.log(request.headers);
						console.log('Request body:');
						console.log(request.body.toString());
						console.log('Response status code '+response.statusCode);
						console.log('Response headers:');
						console.log(response.headers);
					}

					if (response.statusCode != 302) {
						console.log("Unexpected response status code %d on Authorize request.", response.status_code);
						self.is_access_token_accepted = true;
						return
					}
					expect(response.headers.location).toBeDefined();
					if (!response.headers.hasOwnProperty('location')) {
						console.log("Location header is not specified in Authorize response.");
						self.is_access_token_accepted = true;
						return
					}
					var location = response.headers.location;

					expect(response.headers['set-cookie']).toBeDefined();
					var cookie_str = getJSESSSIONID_cookie_str(response);
					expect(cookie_str).not.toBeNull();
					if (cookie_str === null) {
						console.log("Second JSESSIONID not found in Authorize response.");
						if (location.indexOf('login_error') > -1)
						console.log("Check that password '%s' for user '%s' is correct.", password, username);
						self.is_access_token_accepted = true;
						return
					} else
						self.jsession_cookie = cookie_str;

					if (debugLog) console.log('Step 2 success ---------------------------------------');
					frisby.create(my.getStepNumStr() + 'OAuth Get Token')
						.get(location, {followRedirect: false})
						.addHeader('cookie', cookie_str)
						.expectStatus(302)
						.after(function (err, response, body) {
							if (response.statusCode != 302) {
								console.log("Unexpected response status code %d on Get token request.", response.statusCode);
								self.is_access_token_accepted = true;
								return
							}
							expect(response.headers.location).toBeDefined();
							if (!response.headers.hasOwnProperty('location')) {
								console.log("Location header is not specified in Get token response.");
								self.is_access_token_accepted = true;
								return
							}
							location = response.headers.location;

							var rePattern = new RegExp("access_token=[A-Za-z0-9]+-[A-Za-z0-9-]+");
							var matches = location.match(rePattern);
							expect(matches[0]).toBeDefined();
							if (matches != null) {
								self.access_token = matches[0].substring("access_token=".length);
								var exec_time_ms = new Date() - start;
								console.info("\nAccess token '%s' for user '%s' is received within %dms", self.access_token, username, exec_time_ms);
								self.header.Value = 'Bearer ' + self.access_token;
								if (self.doAfter !== null) {
									self.is_access_token_accepted = true;
									self.doAfter();
								}

							} else {
								console.log("'access_token' parameter not found in location header '%s'", location);
								self.is_access_token_accepted = true;
							}
						})
						.toss();

					waitsFor(function () {
							return self.is_access_token_accepted;
						}
						, "Access token do not accepted in time.", 30000);

				})
				.toss();
		})
		.toss();

	return this;

};

OAuth.prototype.then = function(do_after){
	var num_retry = 150;
	var self = this;

	self.doAfter = do_after;
	if (self.access_token !== null)	{
		self.is_access_token_accepted = true;
		self.doAfter();
	}

};

OAuth.prototype.revoke = function() {
	frisby.create(my.getStepNumStr() + 'OAuth revoke')
		.get(this.getRevoke_Url())
		.addHeader(this.header.Name, this.header.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			if (err)
				console.log(err);
			else
				console.log("oAuth revoke token response status code '%d'", response.statusCode)
		})
		.toss();
};

module.exports.makeOAuthClient = function(){return new OAuth()};

function getJSESSSIONID_cookie_str(response) {
	var jsessionid_cookie_str = null;
	if (response.headers.hasOwnProperty('set-cookie'))
		response.headers['set-cookie'].forEach(function (cookie_str) {
			if (cookie_str.indexOf('JSESSIONID') > -1)
			jsessionid_cookie_str = cookie_str;
		});
	return jsessionid_cookie_str;
}