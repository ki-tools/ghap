var my = require('./ums_common');
var frisby = require('frisby');
frisby.globalSetup({timeout:40000});
var umsUrls = require('./ums_urls');

module.exports.createUser = function(authHeader, ums_user, callback){

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Create User ' + ums_user.getName())
		.post(umsUrls.getCreateUser_Url(), ums_user.getCreate_json(),{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Create User ' + ums_user.getName() + ' - check response status code.');
			my.logExecutionTime(start);
			//console.log(body);

			var responseStatus = this.current.response.status;
			if (responseStatus == 400) {
				var body_json = my.jsonParse(body);
				if (body_json.hasOwnProperty('errors')) {
					console.log('>> errors  in response >>');
					console.log(JSON.stringify(body_json.errors));
				}
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();

};

module.exports.deleteUser = function (authHeader, ums_user, callback) {
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Delete User ' + ums_user.getName())
		.delete(umsUrls.getUser_Url( ums_user.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Delete user ' + ums_user.getName() + ' - check response status code.');
			// console.log(body);
			my.logExecutionTime(start);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
};

module.exports.getUser = function(authHeader, ums_user, callback) {
	const EXPECTED_STATUS = 200;
	var start = new Date();
	frisby.create(my.getStepNumStr() + ' Get User')
		.get( umsUrls.getUser_Url(ums_user.getDn()) )
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.expectJSON(ums_user.getUser_json())
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get data for user '+ums_user.getName()+' - check response status and content.');
			my.logExecutionTime(start);

			var responseStatus = this.current.response.status;
			if (responseStatus == EXPECTED_STATUS){
				var response_object = my.jsonParse(body);
				expect(response_object.guid).toBeDefined();
				ums_user.setGuid(response_object.guid);
				console.log(ums_user.getName()+' GUID = '+ums_user.getGuid());
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
};
