var my = require('./ums_common');
var frisby = require('frisby');
frisby.globalSetup({timeout:40000});
var umsUrls = require('./ums_urls');

module.exports.createRole = function(authHeader, ums_role, callback){

	frisby.create(my.getStepNumStr()+' Create New Role')
		.post(umsUrls.getCreateRole_Url(),
			ums_role.getCreateRole_json(),
			{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.expectJSON(ums_role.getRole_json())
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Create role ' + ums_role.name + ' - check response status code and content.');
			console.log(body);

			var responseStatus = this.current.response.status;
			if (responseStatus == 400) {
				var body_json = my.jsonParse(body);
				if (body_json.hasOwnProperty('errors')) {
					console.log('>> errors >>');
					console.log(JSON.stringify(body_json.errors));
				}
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();

};

module.exports.deleteRole = function (authHeader, ums_role, callback) {
		const EXPECTED_STATUS = 200;

		frisby.create(my.getStepNumStr()+' Delete Role')
			.delete(umsUrls.getRole_Url( ums_role.getDn() ))
			.addHeader(authHeader.Name, authHeader.Value)
			.expectStatus(EXPECTED_STATUS)
			.after(function (err, response, body) {
				console.log(my.endOfLine + 'Delete role ' + ums_role.name +' - check response status.');
				console.log(body);

				if (typeof callback === 'function')
					callback( jasmine.getEnv().currentSpec.results().failedCount );
			})
			.toss();

};

module.exports.setRoleToUser = function(authHeader, ums_user, ums_role, callback){

	const EXPECTED_STATUS = 200;

	frisby.create(my.getStepNumStr()+' Set Role To User')
		.get(umsUrls.getAddMemberToRole_Url( ums_role.getDn(), ums_user.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Set role ' + ums_role.name + ' to user ' + ums_user.getName() + ' - check response status code.');

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};

module.exports.getRole = function(authHeader, ums_role, callback){

	const EXPECTED_STATUS = 200;

	frisby.create(my.getStepNumStr()+' Get Role Data')
		.get(umsUrls.getRole_Url( ums_role.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get Role Data for ' + ums_role.name + ' - check response status code.');
			//console.log(body);

			var responseStatus = this.current.response.status;
			if (responseStatus == EXPECTED_STATUS){
				var role_object = my.jsonParse(body);
				expect(role_object.guid).toBeDefined();
				expect(role_object.description).toBeDefined();
				ums_role.guid = role_object.guid;
				console.log(ums_role.name+' UUID = '+ums_role.guid)
				ums_role.description = role_object.description;
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};
