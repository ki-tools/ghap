var Qs = require('qs');
var request = require('request');

var doAfter = null;

function GhapOAuthOld() {
	var
		oauth_Domain = 'http://oauth.dev.ghap.io/',
		oauth_LoginPath = 'oauth2/authorize',
		client_id       = 'projectservice',
		response_type   = 'token',
		redirect_uri    = 'http://oauth.dev.ghap.io/client/client.html';

	this.getAuthorize_Url = function() {
		return oauth_Domain + oauth_LoginPath
			+ '?client_id=' + client_id
			+'&response_type=' + response_type
			+'&redirect_uri=' + redirect_uri;
	};

	this.access_token = null;

	this.header = {
		Name: 'Authorization',
		Value: 'Bearer '
	};

	return this;
}

GhapOAuthOld.prototype.login = function (username, password) {

	var start = new Date();
	var self = this;
	request.get(self.getAuthorize_Url(), function (error, response, body) {
		if (error) {
			console.error('Can not GET %s',self.getAuthorize_Url());
			console.info(error);
			return;
		}
		if (response.statusCode == 200) {
			var pattern_str = 'name="AUTH_STATE" value="';
			var auth_template_str = '7dddaa1c-c401-492c-bdd5-90abc98d0996';

			var pattern_pos = body.indexOf(pattern_str);

			if (pattern_pos > 0) {
				var start_pos = pattern_pos + pattern_str.length;
				var end_pos = start_pos + auth_template_str.length;
				var auth_state_str = body.substring(start_pos, end_pos);
				console.log('AUTH_STATE=' + auth_state_str);

				request({
					url: self.getAuthorize_Url(),
					method: 'POST',
					json: false,
					headers: { 'content-type': 'application/x-www-form-urlencoded' },
					body: Qs.stringify({
						j_username: username,
						j_password: password,
						'user-policy': 'on',
						AUTH_STATE: auth_state_str
					})
				}, function (error, response, body) {
					if (error) {
						console.error('Can not POST data on GHAP oAuth server:');
						console.error(error);
						return;
					}
					if (response.statusCode != 303) {
						console.error('Unexpected response status code %d for POST request on GHAP oAuth server.', response.statusCode);
						console.warn('Username or password may be invalid.');
						return;
					}
					if (!response.headers.hasOwnProperty('location')) {
						console.error("Header 'location' not found in response from GHAP oAuth server");
						return;
					}
					var location = response.headers.location;
					var rePattern =new RegExp("access_token=[A-Za-z0-9]+-[A-Za-z0-9-]+");
					var matches = location.match(rePattern);
					if (matches != null ) {
						self.access_token = matches[0].substring("access_token=".length);
						self.header.Value += self.access_token;
						var exec_time_ms = new Date() - start;
						console.info("Access token '%s' received for user '%s' within %dms", self.access_token, username, exec_time_ms);
						if (doAfter !== null) doAfter();
					} else {
						console.error('Access token not found in response location header.');
						console.log(location);
					}
				});

			}	else
				console.error('AUTH_STATE not found on %s',getAuthorize_Url());

		} else {
			console.error('Unexpected response status code %d for', response.statusCode);
			console.log('GET '+self.getAuthorize_Url());
			//console.log(body);
		}
	});
	return this;
};

GhapOAuthOld.prototype.then = function(do_after) {
	if (this.access_token !== null)
		do_after();
	else
		doAfter = do_after;
};

module.exports = new GhapOAuthOld();