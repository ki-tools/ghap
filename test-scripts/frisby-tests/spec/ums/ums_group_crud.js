var my = require('./../Common/ghap-lib');
var frisby = require('frisby');
frisby.globalSetup({timeout:40000});
var umsUrls = require('./ums_urls');

var ghapFrisby = require('./../Common/ghap-frisby');

module.exports.createGroup =function(authHeader, ums_group, callback) {
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Create New Group', EXPECTED_STATUS)
		.post( umsUrls.getCreateGroup_Url(), ums_group.getCreateGroup_json(), {json: true})
		.expectJSON(ums_group.getGroup_ExpectedJson())
		.setLogMessage("Create group '%s'", ums_group.name)
		.next(callback)
		.returnPromise();

};

module.exports.addMemberToGroup = function(authHeader, ums_user, ums_group, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Add Member To Group', EXPECTED_STATUS)
		.get(umsUrls.getAddMemberToGroup_Url( ums_group.getDn(), ums_user.getDn() ))
		.setLogMessage("Add member '%s' to group '%s'", ums_user.getName(), ums_group.name)
		.onSuccess(function (body) {
			ums_group.addExpectedMember(ums_user);
		})
		.next(callback)
		.returnPromise();
};

module.exports.deleteMemberFromGroup = function(authHeader, ums_user, ums_group, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Delete Member From Group', EXPECTED_STATUS)
		.get(umsUrls.getDeleteMemberFromGroup_Url( ums_group.getDn(), ums_user.getDn() ))
		.setLogMessage("Delete member '%s' from group '%s'", ums_user.getName(), ums_group.name)
		.onSuccess(function (body) {
			ums_group.deleteExpectedMember(ums_user);
		})
		.next(callback)
		.returnPromise();
};


function createGetGroupRequest(authHeader, ums_group){
	const EXPECTED_STATUS = 200;
	return 	ghapFrisby.create(authHeader, 'Get Group Data', EXPECTED_STATUS)
		.get(umsUrls.getGroup_Url( ums_group.getDn() ))
		.setLogMessage("Get data for group '%s'", ums_group.name)
		.onSuccess(function (body) {
			var group_object = my.jsonParse(body);
			expect(group_object.guid).toBeDefined();
			expect(group_object.dn).toBeDefined();
			my.copyProperties(group_object, ums_group);
			console.log("\n%s group GUID = '%s'",ums_group.name, ums_group.guid);
		})
}

module.exports.getGroup = function(authHeader, ums_group, callback){
	const EXPECTED_STATUS = 200;

	return createGetGroupRequest(authHeader, ums_group)
		.expectJSON(ums_group.getGroup_ExpectedJson())
		.next(callback)
		.returnPromise();
};

module.exports.pullGroup = function(authHeader, ums_group, callback){
	return createGetGroupRequest(authHeader, ums_group)
		.setLogMessage("Pull data for group '%s'", ums_group.name)
		.onError(function(response_status, body){
			if (response_status == 404) {
				console.log("Response status code '%d' - group not found.",response_status)
				return true;
			}
			return false;
		})
		.next(callback)
		.returnPromise();
};


module.exports.deleteGroup = function (authHeader, ums_group, callback){

	const EXPECTED_STATUS = 200;

	var start = new Date();
	frisby.create(my.getStepNumStr()+' Delete Group')
		.delete(umsUrls.getGroup_Url( ums_group.getDn() ))
		.addHeader(authHeader.Name, authHeader.Value)
		.expectStatus(EXPECTED_STATUS)
		.after(function (err, response, body) {
			console.log(my.endOfLine + 'Delete group ' + ums_group.name +' - check response status code.');
			my.logExecutionTime(start);

			if (err) console.log(err);

			var response_status = this.current.response.status;
			if (response_status != EXPECTED_STATUS)
				console.log ("Unexpected status code '%d' in Delete Group request. Body:\n<%s>", response_status, body);

			if (typeof callback === 'function')
				callback( jasmine.getEnv().currentSpec.results().failedCount );
		})
		.toss();
};

module.exports.getGroupMembers = function(authHeader, ums_group, callback){
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get Group Members', EXPECTED_STATUS)
		.get(umsUrls.getGroupMembers_Url( ums_group.getDn() ))
		.setLogMessage("Get members of group '%s'", ums_group.name)
		.onSuccess(function (body) {
			ums_group.members  = my.jsonParse(body);
			console.log("\n%s group contains %d members.", ums_group.name, ums_group.members.length);
			//console.log(ums_group.members);
		})
		.next(callback)
		.returnPromise();
};
