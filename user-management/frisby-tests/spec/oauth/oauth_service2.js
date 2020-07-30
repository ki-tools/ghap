var my = require('./../ums/ums_common');
var frisby  = require('frisby');

var is_access_token_accepted = false;

function OAuth(){
	var
		//oauth_Domain = 'http://oauth.dev.ghap.io/',
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

OAuth.prototype.login = function(username, password){

	console.log('OAuth authorization.');
	var self = this;
	var start = new Date();
	//console.log('GET '+ self.getAuthorize_Url());
	frisby.create(my.getStepNumStr() + 'OAuth Get JSESSIONID')
		.get(self.getAuthorize_Url(),
		{followRedirect: false})
		.expectStatus(302)
		.after(function (err, response, body) {
			//console.log('GET response status code '+response.statusCode);
			if (response.statusCode != 302) {
				console.log("Unexpected response status code %d on Get JSESSIONID request.");
				is_access_token_accepted = true;
				return
			}
			expect(response.headers['set-cookie']).toBeDefined();
			var cookie_str = getJSESSSIONID_cookie_str(response);
			if (cookie_str === null) {
				console.log("JSESSIONID not found in GET response.");
				is_access_token_accepted = true;
				return
			}

			//console.log('POST ' + self.getAction_Url());
			frisby.create(my.getStepNumStr() + 'OAuth Authorize')
				.post(self.getAction_Url(), {
					j_username: username,
					j_password: password,
					'user-policy': 'on'
				})
				.addHeader('cookie', cookie_str)
				.expectStatus(302)
				.after(function (err, response, body) {

					//console.log('POST response status code '+response.statusCode);
					//console.log('POST response headers:');
					//console.log(response.headers);
					expect(response.statusCode).toBe(302);
					if (response.statusCode != 302) {
						console.log("Unexpected response status code %d on Authorize request.");
						is_access_token_accepted = true;
						return
					}
					expect(response.headers['set-cookie']).toBeDefined();
					var cookie_str = getJSESSSIONID_cookie_str(response);
					if (cookie_str === null) {
						console.log("Second JSESSIONID not found in Authorize response.");
						is_access_token_accepted = true;
						return
					}
					expect(response.headers.location).toBeDefined();
					if (!response.headers.hasOwnProperty('location')) {
						console.log("Location header is not specified in Authorize response.");
						is_access_token_accepted = true;
						return
					}
					var location = response.headers.location;

					frisby.create(my.getStepNumStr() + 'OAuth Get Token')
						.get(location, {followRedirect: false})
						.addHeader('cookie', cookie_str)
						.expectStatus(302)
						.after(function (err, response, body) {
							if (response.statusCode != 302) {
								console.log("Unexpected response status code %d on Get token request.");
								is_access_token_accepted = true;
								return
							}
							expect(response.headers.location).toBeDefined();
							if (!response.headers.hasOwnProperty('location')) {
								console.log("Location header is not specified in Get token response.");
								is_access_token_accepted = true;
								return
							}
							location = response.headers.location;
							//console.log("GET response.headers.location '%s'", location);

							var rePattern = new RegExp("access_token=[A-Za-z0-9]+-[A-Za-z0-9-]+");
							var matches = location.match(rePattern);
							expect(matches[0]).toBeDefined();
							if (matches != null) {
								self.access_token = matches[0].substring("access_token=".length);
								var exec_time_ms = new Date() - start;
								//is_access_token_accepted = true;
								console.info("Access token '%s' for user '%s' is received within %dms", self.access_token, username, exec_time_ms);
							} else {
								console.log("'access_token' parameter not found in location header '%s'", location);
								is_access_token_accepted = true;
							}
						})
						.toss();

					waitsFor(function () {
							return is_access_token_accepted;
						}
						, "Access token do not accepted in time.", 15000);
				})
				.toss();
		})
		.toss();

	return this;

};

OAuth.prototype.then = function(doAfter){
	var num_retry = 14;
	var self = this;

	function waitMore() {
		if (self.access_token != null) {
			self.header.Value += self.access_token;
			doAfter();
			is_access_token_accepted = true;
		}	else if (num_retry-- > 0)
			setTimeout(waitMore, 1000);
	}
	waitMore();
};

module.exports = new OAuth();

function getJSESSSIONID_cookie_str(response) {
	var jsessionid_cookie_str = null;
	if (response.headers.hasOwnProperty('set-cookie'))
		response.headers['set-cookie'].forEach(function (cookie_str) {
			if (cookie_str.indexOf('JSESSIONID') > -1)
			jsessionid_cookie_str = cookie_str;
		});
	return jsessionid_cookie_str;
}