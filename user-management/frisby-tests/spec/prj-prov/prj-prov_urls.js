exports = module.exports = {};

var
	service_path = 'http://projectservice.dev.ghap.io',
	project_path = '/rest/v1/project';

exports.getAllProjects_Url = function() {
	return service_path + project_path;
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

//-------------------------------------------

exports.getCreateGrant_Url = function(project_id) {
	return service_path + project_path + '/' + project_id + '/grant';
};

exports.getDeleteGrant_Url = function(grant_id) {
	return service_path + project_path + '/grant/' + grant_id;
};
