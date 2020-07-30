var my = require('./../ums/ums_common');
var frisby  = require('frisby');

var is_access_token_accepted = false;

function OAuth(){
	var
		oauth_Domain = 'http://oauth.dev.ghap.io/',
    oauth_LoginPath = 'oauth2/authorize',
		client_id       = 'projectservice',
		//client_id       = 'authorization-server-admin-js-client&scope=read,write',
		response_type   = 'token',
		redirect_uri    = 'http://ghap.io';

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

OAuth.prototype.login = function (username, password) {

	var self = this;
	var start = new Date();
	frisby.create(my.getStepNumStr() + 'OAuth Get Auth State')
		.get(self.getAuthorize_Url())
		.expectStatus(200)
		.after(function (err, response, body) {

			var pattern_str = 'name="AUTH_STATE" value="';
			var auth_template_str = '7dddaa1c-c401-492c-bdd5-90abc98d0996';

			var pattern_pos = body.indexOf(pattern_str);
			expect(pattern_pos).toBeGreaterThan(0);

			if (pattern_pos > 0) {
				var start_pos = pattern_pos + pattern_str.length;
				var end_pos = start_pos + auth_template_str.length;
				var auth_state_str = body.substring(start_pos, end_pos);
				console.log('AUTH_STATE=' + auth_state_str);

				frisby.create(my.getStepNumStr() + 'OAuth Get Token')
					.post(self.getAuthorize_Url(), {
						j_username: username,
						j_password: password,
						'user-policy': 'on',
						AUTH_STATE: auth_state_str
					})
					.expectStatus(303)
					.after(function (err, response, body) {
						var responseStatus = this.current.response.status;
						if (responseStatus == 303) {
							var location = response.headers.location;
							var rePattern = new RegExp("access_token=[A-Za-z0-9]+-[A-Za-z0-9-]+");
							var matches = location.match(rePattern);
							expect(matches).toBeDefined();
							if (matches != null) {
								self.access_token = matches[0].substring("access_token=".length);
								var exec_time_ms = new Date() - start;
								console.info(my.endOfLine + "Access token for user '%s' is received within %dms", username, exec_time_ms);
								console.log('access_token=' + self.access_token);
							}
						}
					})
					.toss();

				waitsFor(function () {
						return is_access_token_accepted;
					}
					, "Access token do not accepted in time.", 15000);
			}
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