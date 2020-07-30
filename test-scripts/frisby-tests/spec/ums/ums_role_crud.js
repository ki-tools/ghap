var my = require('./../Common/ghap-lib');
var frisby = require('frisby');
frisby.globalSetup({timeout:40000});
var umsUrls = require('./ums_urls');

var ghapFrisby = require('./../Common/ghap-frisby');

module.exports.createRole = function(authHeader, ums_role, callback){
	const EXPECTED_STATUS = 200;

	ghapFrisby.create(authHeader, 'Create New Role', EXPECTED_STATUS)
		.post(umsUrls.getCreateRole_Url(), ums_role.getCreateRole_json(),	{json: true})
		.setLogMessage("Create new role '%s'", ums_role.name)
		.onSuccess(function(body){
			my.copyProperties(my.jsonParse(body),ums_role);
		})
		.next(callback)
		.toss();

};

module.exports.deleteRole = function (authHeader, ums_role, callback) {
		const EXPECTED_STATUS = 200;

		ghapFrisby.create(authHeader, 'Delete Role', EXPECTED_STATUS)
			.delete(umsUrls.getRole_Url( ums_role.getDn() ))
			.setLogMessage("Delete role '%s'", ums_role.name)
			.next(callback)
			.toss();
};

module.exports.setRoleToUser = function(authHeader, ums_user, ums_role, callback){

	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Set Role To User', EXPECTED_STATUS)
		.get(umsUrls.getAddMemberToRole_Url( ums_role.getDn(), ums_user.getDn() ))
		.setLogMessage("Set role '%s' to user '%s'", ums_role.name, ums_user.getName())
		.next(callback)
		.returnPromise();
};

module.exports.getRole = function(authHeader, ums_role, callback) {
	const EXPECTED_STATUS = 200;

	ghapFrisby.create(authHeader, 'Get Role Data', EXPECTED_STATUS)
		.get(umsUrls.getRole_Url( ums_role.getDn() ))
		.setLogMessage("Get data for role '%s'", ums_role.name)
		.onSuccess(function(body){
			var role_object = my.jsonParse(body);
			expect(role_object.guid).toBeDefined();
			my.copyProperties(role_object, ums_role);
			console.log("\n%s role GUID = '%s'", ums_role.name, ums_role.guid);
			//my.log(ums_role);

		})
		.next(callback)
		.toss();

};

module.exports.getAllRoles = function(authHeader, allRoles_array, callback) {
	const EXPECTED_STATUS = 200;

	return ghapFrisby.create(authHeader, 'Get All Roles', EXPECTED_STATUS)
		.get(umsUrls.getAllRoles_Url())
		.onSuccess(function(body){
			my.moveArray(my.jsonParse(body),allRoles_array);
			expect(allRoles_array.length).toBeGreaterThan(0);
			if (allRoles_array.length === 0)
				console.error("No roles found.");
			//else
			//	console.log(allRoles_array);
		})
		.next(callback)
		.returnPromise();

};
