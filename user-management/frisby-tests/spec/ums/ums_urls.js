exports = module.exports = {};

var service_path = 'http://userservice.dev.ghap.io',
	signIn_path = '/auth/sign-in',
	signOut_path = '/auth/sign-out',
	user_path = '/user',
	group_path = '/group',
	role_path = '/role';

//  SignIn/Out urls -------------------------------------

exports.getSignIn_GET_Url = function (username, password) {
	var signIn_get_parameters = '?username=' + username + '&password=' + password;
	return service_path + signIn_path + signIn_get_parameters
};

exports.getSignIn_POST_Url = function () {
	return service_path + signIn_path
};

exports.getSignOut_Url = function () {
	return service_path + signOut_path
};

//  User urls --------------------------------------------

exports.getCreateUser_Url = function () {
	return service_path + user_path
};

exports.getUser_Url = function (dn) {
	return service_path + user_path + '/' + dn.toString()
};

exports.getFindAllUsers_Url  = function (group_dn) {
	return service_path + user_path + '/all/' + group_dn.toString()
};

//  Group urls --------------------------------------------

exports.getCreateGroup_Url = function () {
	return service_path + group_path
};

exports.getGroup_Url = function (dn) {
	return service_path + group_path + '/' + dn.toString()
};

exports.getAddMemberToGroup_Url = function (group_dn, member_dn) {
	return service_path + group_path + '/' + group_dn.toString() + '/add/' + member_dn.toString()
};

exports.getDeleteMemberFromGroup_Url = function (group_dn, member_dn) {
	return service_path + group_path + '/' + group_dn.toString() + '/delete/' + member_dn.toString()
};

exports.getGroupMembers_Url = function (group_dn) {
	return service_path + group_path + '/members/' + group_dn.toString()
};

//  Role urls --------------------------------------------

exports.getCreateRole_Url = function () {
	return service_path + role_path
};

exports.getRole_Url = function (role_dn) {
	return service_path + role_path + '/' + role_dn.toString()
};

exports.getAddMemberToRole_Url = function (role_dn, member_dn) {
	return service_path + role_path + '/' + role_dn.toString() + '/add/' + member_dn.toString()
};

exports.getDeleteMemberFromRole_Url = function (role_dn, member_dn) {
	return service_path + role_path + '/' + role_dn.toString() + '/delete/' + member_dn.toString()
};

exports.getRoleMembers_Url = function (role_dn) {
	return service_path + role_path + '/members/' + role_dn.toString()
};
