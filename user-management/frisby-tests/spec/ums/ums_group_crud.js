var my = require('./ums_common');
var frisby = require('frisby');
frisby.globalSetup({timeout:40000});
var umsUrls = require('./ums_urls');

module.exports.createGroup =function(authHeader, ums_group, callback) {

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Create New Group')
		.post( umsUrls.getCreateGroup_Url(),
		ums_group.getCreateGroup_json(),
		{json: true})
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(200)
		.expectJSON(ums_group.getGroup_json())
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Create group ' + ums_group.name + ' - check response status code and content.');
			//console.log(body);

			var exec_time_ms = new Date() - start;
			if (exec_time_ms > 5000)
				console.info("WARNING: Execution time is too long: %dms", exec_time_ms);

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

module.exports.addMemberToGroup = function(authHeader, ums_user, ums_group, callback){

	const EXPECTED_STATUS = 200;

	frisby.create(my.getStepNumStr()+' Add Member To Group')
		.get(umsUrls.getAddMemberToGroup_Url( ums_group.getDn(), ums_user.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Add member ' + ums_user.getName() + ' to group ' + ums_group.name + ' - check response status code.');

			var responseStatus = this.current.response.status;
			if (responseStatus == EXPECTED_STATUS)
				ums_group.addExpectedMember(ums_user);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};

module.exports.deleteMemberFromGroup = function(authHeader, ums_user, ums_group, callback){

	const EXPECTED_STATUS = 200;

	frisby.create(my.getStepNumStr()+' Delete Member From Group')
		.get(umsUrls.getDeleteMemberFromGroup_Url( ums_group.getDn(), ums_user.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Delete member ' + ums_user.getName() + ' from group ' + ums_group.name + ' - check response status code.');

			var responseStatus = this.current.response.status;
			if (responseStatus == EXPECTED_STATUS)
				ums_group.deleteExpectedMember(ums_user);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};

module.exports.getGroup = function(authHeader, ums_group, callback){

	const EXPECTED_STATUS = 200;

	frisby.create(my.getStepNumStr()+' Get Group Data')
		.get(umsUrls.getGroup_Url( ums_group.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.expectJSON(ums_group.getGroup_json('CN'))
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get group data - check response status code and content.');
			//console.log(body);

			if (this.current.response.status == EXPECTED_STATUS){
				var group_object = my.jsonParse(body);
				expect(group_object.guid).toBeDefined();
				expect(group_object.description).toBeDefined();
				ums_group.guid = group_object.guid;
				console.log(ums_group.name+' UUID = '+ums_group.guid);
				ums_group.description = group_object.description;
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
};

module.exports.deleteGroup = function (authHeader, ums_group, callback){

	const EXPECTED_STATUS = 200;

	frisby.create(my.getStepNumStr()+' Delete Group')
		.delete(umsUrls.getGroup_Url( ums_group.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Delete group ' + ums_group.name +' - check response status.');
			//console.log(body);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
};

module.exports.getGroupMembers = function(authHeader, ums_group, callback){

	const EXPECTED_STATUS = 200;

	frisby.create(my.getStepNumStr()+' Get Group Members')
		.get(umsUrls.getGroupMembers_Url( ums_group.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Get members of group ' + ums_group.name + ' - check response status code.');
			//console.log(body);

			var responseStatus = this.current.response.status;
			if (responseStatus == EXPECTED_STATUS) {
				ums_group.members  = my.jsonParse(body);
				console.log(ums_group.name+' group contain '+ums_group.members.length+' members.')
			}

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );

		})
		.toss();
};
