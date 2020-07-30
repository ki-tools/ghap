exports = module.exports = {};

var
	service_path = 'http://activityservice.dev.ghap.io',
	activity_path = '/rest/v1/Activity',
	activityRoleAssociation_path = '/rest/v1/ActivityRoleAssociation'

exports.getAllActivities_Url = function () {
	return service_path + activity_path + '/get'
};

exports.getActivityById_Url = function (activity_id) {
	return service_path + activity_path + '/get/id/'+activity_id;
};

exports.getActivityByName_Url = function (activity_name) {
	return service_path + activity_path + '/get/name/'+activity_name;
};

exports.getCreateActivity_Url = function (activity_name, min_units, max_units, default_units) {
	var param_str = '';
	if (typeof activity_name === 'string')
		param_str = '/' + activity_name +'/'+ min_units +'/'+ max_units +'/'+ default_units;
	return service_path + activity_path + '/create' + param_str
};

exports.getDeleteActivity_Url = function () {
	return service_path + activity_path + '/delete/activity'
};

exports.getDeleteActivityById_Url = function (uuid) {
	return service_path + activity_path + '/delete/id/'+uuid
};

exports.getAssociateActivityWithRole_Url = function (activity_name, role_uuid) {
	return service_path + activityRoleAssociation_path + '/create/'+activity_name+'/'+role_uuid
};

exports.getARAssociationsForRole_Url = function (role_uuid) {
	return service_path + activityRoleAssociation_path + '/get/' +role_uuid
};
