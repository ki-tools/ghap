exports = module.exports = {};

var
	service_path = 'http://provisioningservice.dev.ghap.io',
	personalStorage_path = '/rest/v1/PersonalStorage',
	vpg_path             = '/rest/v1/VirtualPrivateGrid';

exports.getCreateStorage_Url = function(user_uid, size_gb) {
	var param_str = '';
	if (typeof user_uid !== 'undefined')
		param_str = '/'+user_uid+'/'+size_gb;
	return service_path + personalStorage_path + '/create' + param_str;
};

exports.getGetStorage_Url = function (user_uid) {
	return service_path + personalStorage_path + '/get/'+user_uid;
};

exports.getDeleteStorage_Url = function (user_uid) {
	return service_path + personalStorage_path + '/delete/'+user_uid;
};

exports.getExistsStorage_Url = function (user_uid) {
	return service_path + personalStorage_path + '/exists/'+user_uid;
};

//-------------------------------------------------------

exports.getCreateVPG_Url = function(user_uid) {
	var param_str = '';
	if (typeof user_uid !== 'undefined') param_str = '/'+user_uid;
	return service_path + vpg_path + '/create' + param_str;
};

exports.getExistsVPG_Url = function (user_uid) {
	return service_path + vpg_path + '/exists/'+user_uid;
	//return service_path + vpg_path + '/exists/619469ea-4081-4cb6-8fd6-86b3bb0af355';
};

exports.getTerminateVPG_Url = function (user_uid) {
	return service_path + vpg_path + '/terminate/'+user_uid;
};

exports.getAllVPG_Url = function() {
	return service_path + vpg_path + '/get';
};
