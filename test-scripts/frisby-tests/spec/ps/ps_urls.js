exports = module.exports = {};

var cfg = require('./../Common/ghap-config');

var
	service_path = cfg.provisioningservice,
	personalStorage_path = '/rest/v1/PersonalStorage',
	vpg_path             = '/rest/v1/VirtualPrivateGrid',
	multi_vpg_path       = '/rest/v1/MultiVirtualPrivateGrid';

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

//-------------------------------------------------------------

exports.getCreateVPG_Url = function(user_uid) {
	var param_str = '';
	if (typeof user_uid !== 'undefined') param_str = '/'+user_uid;
	return service_path + vpg_path + '/create' + param_str;
};

exports.getExistsVPG_Url = function (user_uid) {
	return service_path + vpg_path + '/exists/'+user_uid;
};

exports.getVPG_Url = function (user_uid) {
	return service_path + vpg_path + '/get/'+user_uid;
};

exports.getComputeResources_Url = function (vpg_id) {
	return service_path + vpg_path + '/get/compute/'+vpg_id;
};

exports.getVPG_status_Url = function (user_uid) {
	return service_path + vpg_path + '/status/'+user_uid;
};

exports.getTerminateVPG_Url = function (user_uid) {
	return service_path + vpg_path + '/terminate/'+user_uid;
};

exports.getAllVPG_Url = function() {
	return service_path + vpg_path + '/get';
};

//--------------------------------------------------------------

/**
 * Return URL of provisioning service request to get array of the stacks that currently exist.
 * @returns {string}
 */
exports.multiVPG_getAllStacks = function () {
	return service_path + multi_vpg_path + '/get';
};

/**
 * Return URL of provisioning service request to get array of the stacks for the provided user id.
 * @param user_uid
 * @returns {string}
 */
exports.multiVPG_getStack4user = function (user_uid) {
	return service_path + multi_vpg_path + '/get/' + user_uid;
};

/**
 * Return URL of provisioning service request to create VPG for specified user based on activity provided in the request body.
 * @param user_uid
 * @returns {string}
 */
exports.multiVPG_createVPG = function (user_uid) {
	return service_path + multi_vpg_path + '/create/' + user_uid;
};

/**
 * Return URL of provisioning service request to get array of compute resources for the specified VPG
 * @param vpg_id
 * @returns {string}
 */
exports.multiVPG_getComputeResources4Stack = function (vpg_id) {
	return service_path + multi_vpg_path + '/get/compute/' + vpg_id;
};

/**
 * Return URL of provisioning service request to get array of compute resources for the specified user
 * @param user_id
 * @returns {string}
 */
exports.multiVPG_getComputeResources4user = function (user_id) {
	return service_path + multi_vpg_path + '/get/user/compute/' + user_id;
};
/**
 * Return URL of provisioning service request to get array of compute resources for the specified user
 * @param user_id
 * @returns {string}
 */

/**
 * Return URL of provisioning service request to get the compute resources that are currently provisioned
 * @returns {string}
 */
exports.multiVPG_getAllComputeResources = function () {
	return service_path + multi_vpg_path + '/get/user/compute/';
};

/**
 * Return URL of provisioning service request to get all instances registered in AWS
 * @returns {string}
 */
exports.multiVPG_getAllAwsInstances = function () {
	return service_path + multi_vpg_path + '/get/resources/aws';
};

/**
 * Return URL of provisioning service request to stop VPG for specified user based on activity provided in the request body.
 * @param user_uid
 * @returns {string}
 */
exports.multiVPG_pause = function (user_uid) {
	return service_path + multi_vpg_path + '/pause/' + user_uid;
};

/**
 * Return URL of provisioning service request to resume VPG for specified user based on activity provided in the request body.
 * @param user_uid
 * @returns {string}
 */
exports.multiVPG_resume = function (user_uid) {
	return service_path + multi_vpg_path + '/resume/' + user_uid;
};

/**
 * Return URL of provisioning service request to terminate VPG for specified user and activity
 * @param user_uid
 * @param activity_id
 * @returns {string}
 */
exports.multiVPG_terminate = function (user_uid, activity_id) {
	return service_path + multi_vpg_path + '/terminate/' + user_uid + '/' + activity_id;
};

/**
 * Return URL of provisioning service request to get RDP file
 * @param instance_id
 * @param instance_os_type
 * @param ip_address
 * @returns {string}
 */
exports.multiVPG_rdpFile = function (instance_id, instance_os_type, ip_address) {
	return service_path + multi_vpg_path + '/rdp-file/' + instance_id + '/' + instance_os_type + '?ipAddress=' + ip_address;
};