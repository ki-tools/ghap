exports = module.exports = {};

var cfg = require('./../Common/ghap-config');

var
	//service_path = 'http://projectservice.dev.ghap.io',
	//service_path = 'http://projectservice.qa.ghap.io',
	service_path = cfg.projectservice,
	project_path = '/rest/v1/project',
	directStash_path = '/rest/v1/directStash';

exports.getAllProjects_Url = function() {
	return service_path + project_path;
};

exports.getProjectUsersPermissions_Url = function(project_id) {
	return service_path + project_path + '/' + project_id + '/users';
};

exports.getAllProjects4User_Url = function(user_id) {
	return service_path + project_path + '/' + user_id;;
};

exports.getAllGrantsOfProject4User_Url = function(project_id, user_id) {
	return service_path + project_path + '/' + project_id + '/grants/' + user_id;
};

exports.getCreateProject_Url = function() {
	return service_path + project_path;
};

exports.getDeleteProject_Url = function(project_id) {
	return service_path + project_path + '/' + project_id;
};

exports.getAllGrants_Url = function(project_id) {
	return service_path + project_path + '/' + project_id + '/grants';
};

exports.getGrantProjectPermissions_Url= function(project_id, user_id) {
	return service_path + project_path + '/' + project_id + '/grantProjectPermissions/' + user_id;
};

exports.getRevokeProjectPermissions_Url= function(project_id, user_id) {
	return service_path + project_path + '/' + project_id + '/revokeProjectPermissions/' + user_id;
};

//-------------------------------------------

exports.getCreateGrant_Url = function(project_id) {
	return service_path + project_path + '/' + project_id + '/grant';
};

exports.getDeleteGrant_Url = function(grant_id) {
	return service_path + project_path + '/grant/' + grant_id;
};

exports.getGrantGrantPermissions_Url= function(grant_id, user_id) {
	return service_path + project_path + '/' + grant_id + '/grantGrantPermissions/' + user_id;
};

exports.getRevokeGrantPermissions_Url= function(grant_id, user_id) {
	return service_path + project_path + '/' + grant_id + '/revokeGrantPermissions/' + user_id;
};

exports.getAllUsers4Grant_Url = function(grant_id) {
	return service_path + project_path + '/grant/' + grant_id + '/users';
};

//-------------------------------------------

exports.getAllStashProjects_Url = function() {
	return service_path + directStash_path + '/get';
};

exports.getAllStashGrants4Project_Url = function(stash_project_key) {
	return service_path + directStash_path + '/get/' + stash_project_key;
};

/* @Path("/get/{projectKey}/permissions/{username}") */
exports.getStashProjectPermissions4User_Url = function(stash_project_key, stash_username) {
	return service_path + directStash_path + '/get/' + stash_project_key + '/permissions/' + stash_username;
};

/* @Path("/get/{projectKey}/permissions/{username}/repo/{slug}") */
exports.getStashGrantPermissions4User_Url = function(stash_project_key, stash_username, stash_repo_slug) {
	return service_path + directStash_path + '/get/' + stash_project_key + '/permissions/'
			+ stash_username + '/repo/' + stash_repo_slug;
};

/* @Path("/get/{projectKey}/permissions") */
exports.getStashProjectUsersPermissions_Url = function(stash_project_key) {
	return service_path + directStash_path + '/get/' + stash_project_key + '/permissions';
};

/* @Path("/get/{projectKey}/permissions/repo/{slug}") */
exports.getStashGrantUsersPermissions_Url = function(stash_project_key, stash_repo_slug) {
	return service_path + directStash_path + '/get/' + stash_project_key + '/permissions/repo/'
		+ stash_repo_slug;
};

/* @Path("/fileExists/{fileName}") */
exports.isFileExistsInStash_Url = function(file_name) {
	return service_path + directStash_path + '/fileExists/' + encodeURIComponent(file_name);
};
