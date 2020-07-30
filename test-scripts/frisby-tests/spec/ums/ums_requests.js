var Q = require('q');

var my = require('./../Common/ghap-lib');
var ghapFrisby = require('./../Common/ghap-frisby');

var umsUrls = require('./ums_urls');
var umsUser = require('./ums_user');

module.exports.createUser = function(authHeader, ums_user, callback){
	const EXPECTED_STATUS = 200;
	return ghapFrisby.create(authHeader, 'Create User', EXPECTED_STATUS)
		.post(umsUrls.getCreateUser_Url(), ums_user.getCreate_json(),{json: true})
		.setLogMessage('Create user %s', ums_user.getName())
		.onSuccess(function(body){
			umsUser.makeUserFromGhapUser(my.jsonParse(body), ums_user);
		})
		.next(callback)
		.returnPromise();
};

module.exports.deleteUser = function (authHeader, ums_user, callback) {
	const EXPECTED_STATUS = 200;
	return ghapFrisby.create(authHeader, 'Delete User', EXPECTED_STATUS)
		.delete(umsUrls.getUser_Url( ums_user.getDn() ))
		.setLogMessage('Delete user %s',ums_user.getName())
		.next(callback)
		.returnPromise();
};

module.exports.getUser = function(authHeader, ums_user, callback) {
	const EXPECTED_STATUS = 200;
	return ghapFrisby.create(authHeader, 'Get User', EXPECTED_STATUS)
		.get( umsUrls.getUser_Url(ums_user.getDn()) )
		.setLogMessage('Get data of user %s',ums_user.getName())
		.expectJSON(ums_user.getUser_ExpectedJson())
		.onSuccess(function(body){
			var ghap_user = my.jsonParse(body);
			expect(ghap_user.guid).toBeDefined();
			umsUser.makeUserFromGhapUser(ghap_user, ums_user);
		})
		.next(callback)
		.returnPromise();
};

/**
 * @typedef {object} ghapUser
 * @property {string} dn             'CN=e2e user,CN=Users,DC=prod,DC=ghap,DC=io'
 * @property {string} objectClass    'user'
 * @property {string} guid           'f8aa52cf-0500-459c-abef-06961543bccc'
 * @property {string} fullName       'e2e user'
 * @property {number} passwordExpiresDate -11636784000000
 * @property {number} pwdLastSet      -11644560000000
 * @property {number} badPasswordTime -11644560000000
 * @property {number} badPwdCount     0
 * @property {string} name            'vlad.ruzov.e2e'
 * @property {string} firstName       'e2e'
 * @property {string} lastName        'user'
 * @property {string} email           'ghap.tester@gmail.com'
 * @property {boolean} locked
 * @property {boolean} resetPassword
 * @property {boolean} passwordNeverExpire
 * @property {boolean} passwordExpiredFlag
 * @property {boolean} disabled
 */

/**
 * Push all Ghap users to allGhapUsers array
 * @param {GhapAuthHeader} authHeader
 * @param {string} parentDn_str
 * @param {ghapUser[]} allGhapUsers_array - RESULTS
 * @param {Function} [callback]
 * @returns {Promise}
 */
module.exports.getAllUsers = function(authHeader, parentDn_str, allGhapUsers_array, callback) {
	return ghapFrisby.create(authHeader, 'Get All Users')
		.get(umsUrls.getFindAllUsers_Url( parentDn_str ))
		.onSuccess(function(body){
			my.moveArray(my.jsonParse(body), allGhapUsers_array);
			console.log(" %d users received.", allGhapUsers_array.length)
		})
		.next(callback)
		.returnPromise();
};

module.exports.updateUser = function(authHeader, ums_user, new_values_json, callback) {
	const EXPECTED_STATUS = 200;
	ghapFrisby.create(authHeader, 'Update User Data', EXPECTED_STATUS)
		.post( umsUrls.getUser_Url( ums_user.getDn() ), new_values_json, {json: true})
		.setLogMessage('Update data of user %s',ums_user.getName())
		.onSuccess(function(body){
			var updateData_result = ums_user.updateUserData(new_values_json);
			expect(updateData_result).toBe(true);
			this.next(null);
			exports.getUser(authHeader, ums_user, callback);
		})
		.next(callback)
		.toss();
};

module.exports.getUserRoles = function (authHeader, ums_user, callback) {
	const EXPECTED_STATUS = 200;
	var url_str = umsUrls.getUserRoles_Url( ums_user.getDn() );
	return ghapFrisby.create(authHeader, 'Get User Roles', EXPECTED_STATUS)
		.get(url_str)
		.setLogMessage('Get roles of user %s', ums_user.getName())
		.onSuccess(function(body){
			ums_user.setRoles(my.jsonParse(body));
			console.log("\nUser have %d roles", ums_user.getRoles().length);
		})
		.next(callback)
		.returnPromise();
};

module.exports.pullUserData = function(authHeader, ums_user, callback, omError_callback) {
	const EXPECTED_STATUS = 200;
	return ghapFrisby.create(authHeader, 'Pull user data', EXPECTED_STATUS)
		.get( umsUrls.getUser_Url(ums_user.getDn()) )
		.setLogMessage('Pull data for %s',ums_user.getDn())
		.onSuccess(function(body){
			var ghap_user = my.jsonParse(body);
			expect(ghap_user.guid).toBeDefined();
			umsUser.makeUserFromGhapUser(ghap_user, ums_user);
		})
		.onError(function(response_status, body){
			console.log(umsUrls.getUser_Url(ums_user.getDn()));
			if (typeof omError_callback === 'function'){
				this.next(null); // to prevent onSuccess callback
				return omError_callback(response_status, body);
			}
			if (response_status == 404) {
				console.log("Response status code 404. User not found.");
				return true;
			}
			return false;
		})
		.next(callback)
		.returnPromise();
};

module.exports.resetUserPassword = function(authHeader, ums_user, new_password_str, callback) {
	const EXPECTED_STATUS = 204;
	return ghapFrisby.create(authHeader, 'Reset User password', EXPECTED_STATUS)
		.post( umsUrls.getResetUserPassword_Url(ums_user.getDn()), {password: new_password_str}, {json: true} )
		.setLogMessage("Reset password for '%s' to '%s'",ums_user.getName(), new_password_str)
		.next(callback)
		.returnPromise();
};

module.exports.getCurrentUser = function(authHeader, ums_user, callback) {
	const EXPECTED_STATUS = 200;
	return ghapFrisby.create(authHeader, 'Get Current User', EXPECTED_STATUS)
		.get( umsUrls.getCurrentUser_Url() )
		.setLogMessage("Get Current user for token '%s'",authHeader.Value.substring(7))
		.onSuccess(function(body){
			var ghap_user = my.jsonParse(body);
			expect(ghap_user.guid).toBeDefined();
			umsUser.makeUserFromGhapUser(ghap_user, ums_user);
			if (ghap_user.guid)
				console.log("\nCurrent user '%s' with guid '%s'", ums_user.getName(), ums_user.getGuid());
		})
		.onError(function(){
			var response_status = this.current.response.status;
			if (response_status == 401){
				console.log("Response status code 401 on Get Current User request. Probably token is expired.");
				return true; // to prevent err logging
			}
			return false;
		})
		.next(callback)
		.returnPromise();
};

module.exports.validateToken = function(o_auth){
	var current_user = umsUser.makeUser('any');
	return exports.getCurrentUser(o_auth.header, current_user)
		.then( function(){
			if (current_user.getGuid() === 'unknown'){
				return o_auth.login(o_auth.username, o_auth.password)
			}
		})
};

module.exports.pullUser = function (o_auth, ums_user) {
    var deferred = Q.defer();
    exports.pullUserData(o_auth.header, ums_user,
        function () {  // on Success
            deferred.resolve(waits(300));
        },
        function () {  // on Error
            deferred.reject(new Error("Can`t pull data for user '" + ums_user.getName() + "'."));
        }
    );
    return deferred.promise;
};
