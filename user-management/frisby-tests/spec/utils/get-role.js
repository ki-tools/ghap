var request = require('request');

var testerAdmin = require('../ums/tester_admin');
var oAuth = require('./ghap-oauth-old');
var
	//admin_username = 'Administrator',
	//admin_password = '';
  //admin_username = 'Tester.Admin',
  //admin_password = '';
	admin_username = testerAdmin.getName(),
	admin_password = testerAdmin.getPassword();

var umsRole = require('../ums/ums_role');
var umsUrls = require('../ums/ums_urls');

role = umsRole.makeRole('BMGF Administrator');

oAuth.login(admin_username,admin_password)
	.then(function () {
		var headers = {};
		headers['content-type'] = 'application/json';
		headers[oAuth.header.Name] = oAuth.header.Value;
		request.get({
				url: umsUrls.getRole_Url(role.getDn()),
				headers: headers
			}
			, function (error, response, body) {
				console.log(response.statusCode);
				console.log(body);
			}
		);
	});
