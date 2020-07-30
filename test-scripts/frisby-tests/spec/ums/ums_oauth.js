exports = module.exports = {};

var my = require('./../Common/ghap-lib');
var frisby  = require('frisby');

var
	oauthDomain = 'http://oauth.dev.ghap.io/',
	//oauthDomain = 'http://ghap-oauth-us-east-1-dev.elasticbeanstalk.com/',
	oauthLoginPath  = 'oauth2/authorize',
	oauthLoginParameters = '?client_id=projectservice&response_type=token&redirect_uri=http://ghap.io';
	//oauthLoginParameters = '?client_id=authorization-server-admin-js-client&scope=read,write&response_type=token&redirect_uri=http://ghap.io';

var access_token;
var is_access_token_accepted = false;

var start = new Date();

frisby.create(my.getStepNumStr()+'OAuth Get Auth State')
	.get(oauthDomain+oauthLoginPath+oauthLoginParameters)
	.expectStatus(200)
	.after(function (err, response, body) {

		var pattern_str = 'name="AUTH_STATE" value="';
		var auth_template_str = '7dddaa1c-c401-492c-bdd5-90abc98d0996';

		var pattern_pos = body.indexOf(pattern_str);
		expect(pattern_pos).toBeGreaterThan(0);

		if (pattern_pos > 0) {
			var start_pos = pattern_pos + pattern_str.length;
			var end_pos = start_pos + auth_template_str.length;
			var auth_state_str = body.substring(start_pos,end_pos);
			console.log('AUTH_STATE='+auth_state_str);

			frisby.create(my.getStepNumStr()+'OAuth Get Token')
				.post(oauthDomain+oauthLoginPath+oauthLoginParameters,{
					j_username: my.admin_username,
					j_password: my.admin_password,
					'user-policy': 'on',
					AUTH_STATE: auth_state_str
				})
				.expectStatus(303)
				.after(function (err, response, body) {
					var responseStatus = this.current.response.status;
					if (responseStatus == 303){
						var location = response.headers.location;
						var rePattern =new RegExp("access_token=[A-Za-z0-9]+-[A-Za-z0-9-]+");
						var matches = location.match(rePattern);
						expect(matches).toBeDefined();
						if (matches != null ) {
							access_token = matches[0].substring("access_token=".length);
							var exec_time_ms = new Date() - start;
							console.info(my.endOfLine+"Access token is received within %dms", exec_time_ms);
							console.log('access_token='+access_token);
						}
					}
				})
				.toss();

			waitsFor(function () { return is_access_token_accepted;	}
				, "Access token do not accepted in time.", 15000);
		}
	})
	.toss();

exports.access_token = '';
exports.header = {
	Name: 'Authorization',
	Value: 'Bearer '
};

exports.waitAccessToken = function(doAfter){
	var num_retry = 14;

	function waitMore() {
		if (access_token != null) {
			exports.access_token = access_token;
			exports.header.Value += access_token;
			doAfter();
			is_access_token_accepted = true;
		}	else if (num_retry-- > 0)
			setTimeout(waitMore, 1000);
	}
	waitMore();

};