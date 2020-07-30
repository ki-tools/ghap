var my = require('./../Common/ghap-lib');
var frisby = require('frisby');
var umsUrls = require('./ums_urls');

var umsUser = require('./ums_user');
var Tester = umsUser.makeUser('MrTester');

var oAuth;
oAuthService = require('./../oauth/oauth_service2');
oAuth = oAuthService.makeOAuthClient();
var testerAdmin = require('./tester_admin');
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(findAllUsers);

// findAllUsers
function findAllUsers() {
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Find All Users')
		.timeout(40000)
		.get(umsUrls.getFindAllUsers_Url( Tester.getParentDn() ))
		.addHeader(oAuth.header.Name, oAuth.header.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Find all users - check response status code and content.');
			console.log(body);

			var exec_time_ms = new Date() - start;
			if (exec_time_ms > 5000)
				console.info("WARNING: Execution time is too long: %dms", exec_time_ms);

			var responseStatus = this.current.response.status;
			if (responseStatus == 200) {
				var resp_json = my.jsonParse(body);
				expect(resp_json.length).toBeGreaterThan(3);
				var names = ['Guest'];
				var filtered = resp_json.filter(function(entry) {
					// console.log(entry);
					if (names.indexOf(entry.name) !== -1)
					  return true;
				} );
				//if (logInfo) console.log(filtered);
				expect(filtered.length).toEqual(1);
			}

		})
		.toss();
}