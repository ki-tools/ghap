var frisby = require('frisby');

var endOfLine = require('os').EOL;

var username = 'GHAPAdministrator';
var password = '';
var wrongPassword = '';

var Ums = (function () {
	var service_path = 'http://userservice.dev.ghap.io',
		signIn_path = '/auth/sign-in',
		signOut_path = '/auth/sign-out',

		getSignIn_GET_Url, getSignIn_POST_Url, getSignOut_Url;

	getSignIn_GET_Url = function (username, password) {
		var signIn_get_parameters = '?username=' + username + '&password=' + password;
		return service_path + signIn_path + signIn_get_parameters
	};

	getSignIn_POST_Url = function () {
		return service_path + signIn_path
	};

	getSignOut_Url = function () {
		return service_path + signOut_path
	};

	return {
		getSignIn_GET_Url: getSignIn_GET_Url,
		getSignIn_POST_Url: getSignIn_POST_Url,
		getSignOut_Url: getSignOut_Url
	};
}());

// Step001
frisby.create('001 SignIn GET check Status Code')
  .get( Ums.getSignIn_GET_Url(username,password) )
  .expectStatus(200)
  .after( function(err,response001,body) {
		console.log("SignIn(GET)as %s and check response status code",username);
		//console.log('signIn request Headers:');
		//console.log(this.current.request.headers)
		//console.log('signIn response001 Headers:');
		//console.log(response001.headers);
		//console.log('---------------------------');
		var responseStatus = this.current.response.status;

		if (responseStatus != 200) return;

		doSteps002toEnd(response001);
	})
	.toss();

var doSteps002toEnd = function(response001) {

	frisby.create('002 SignOut Of Signed User')
		.get(Ums.getSignOut_Url())
		.addHeader('Cookie', response001.headers['set-cookie'])
		.expectStatus(204)
		.after(function (err, response, body) {
			console.log(endOfLine + 'SignOut of signed user - check response status code');
			//	console.log('signOut request Headers:');
			//	console.log(this.current.request.headers)
			//	console.log('signOut response Headers:');
			//	console.log(response.headers);

			doSteps003toEnd(response001);
		})
		.toss();
};

var doSteps003toEnd = function(response001) {

	frisby.create('003 SignOut Of Unsigned User')
		.get(Ums.getSignOut_Url())
		.addHeader('Cookie', response001.headers['set-cookie'])
		.expectStatus(401)
		.after(function (err, response, body) {
			//console.log('signOut response Headers:');
			//console.log(response.headers);
			console.log(endOfLine + 'SignOut of unsigned user - check response status code. Response body:');
			console.log(body);

			doSteps004toEnd();
		})
		.toss();
};

var doSteps004toEnd = function() {

	frisby.create('004 SignIn GET check JSON response')
		.get(Ums.getSignIn_GET_Url(username, password))
		.expectJSON({"dn": 'CN=' + username + ',CN=Users,DC=prod,DC=ghap,DC=io', "name": username})
		.after(function (err, response001, body) {
			console.log(endOfLine + 'SignIn(GET) as %s and check response content. Response body:', username);
			console.log(body);

			frisby.create('004 SignOut Of Signed User')
				.get(Ums.getSignOut_Url())
				.addHeader('Cookie', response001.headers['set-cookie'])
				.expectStatus(204)
				.after(function (err, response, body) {
					console.log(endOfLine + 'SignOut Of signed user - check response status code');

					doSteps005toEnd();
				})
				.toss();
		})
		.toss();
};

var doSteps005toEnd = function() {

	frisby.create('005 SignIn POST')
		.post(Ums.getSignIn_POST_Url(), {
			username: username,
			password: password
		})
		.expectStatus(200)
		.expectJSON({"dn": 'CN=' + username + ',CN=Users,DC=prod,DC=ghap,DC=io', "name": username})
		.after(function (err, response001, body) {
			console.log(endOfLine + 'SignIn(POST) as %s and check  response status code and content. Response body:', username);
			console.log(body);

			frisby.create('005 SignOut Of Signed User')
				.get(Ums.getSignOut_Url())
				.addHeader('Cookie', response001.headers['set-cookie'])
				.expectStatus(204)
				.after(function () {
					console.log(endOfLine + 'SignOut Of signed user');

					doSteps006toEnd()
				})
				.toss();
		})
		.toss();
};

var doSteps006toEnd = function() {
	frisby.create('006 SignIn with wrong password')
		.post(Ums.getSignIn_POST_Url(), {
			username: username,
			password: wrongPassword
		})
		.expectStatus(401)
		.after(function (err, response, body) {
			console.log(endOfLine + 'SignIn as %s with wrong password - check response status code. Response body:', username);
			console.log(body);
		})
		.toss();
};
