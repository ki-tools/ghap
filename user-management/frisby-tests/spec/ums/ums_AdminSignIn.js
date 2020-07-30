var my = require('./ums_common');
var frisby = require('frisby');
var umsUrls = require('./ums_urls');

var doAdminSignIn = function(doAfter) {

	frisby.create( my.getStepNumStr()+' SignIn As Admin User')
		.post(umsUrls.getSignIn_POST_Url(), {
			username: my.admin_username,
			password: my.admin_password
		})
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'SignIn(POST) as %s and check response status code.', my.admin_username);

			var responseStatus = this.current.response.status;
			if (responseStatus != 200) return;
			if(typeof doAfter ==='function') doAfter( response.headers['set-cookie'].toString() );
		})
		.toss();
};

module.exports = doAdminSignIn;