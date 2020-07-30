/**
 * Created by Vlad on 28.05.2015.
 */
var configJson = require('./ghap-config.json');

var environment_name = configJson.defaultEnvironment;
if (process.env.GHAP_ENV) environment_name = process.env.GHAP_ENV;

var config = configJson[environment_name];
console.log("Default configuration is '%s'.", environment_name);
config.environment = environment_name;

if (!checkConfig(config)) {
	console.error('Invalid configuration file.');
	process.exit(1);
}

// node ignore ssl
//
process.env.NODE_TLS_REJECT_UNAUTHORIZED = "0";

setConfigValues(config);

function checkConfig(config){

	const config_properties = [
		'origin',           'environment',         'oauthDomain',
		'userservice',      'userdataService',     'projectservice',
		'stashRepoBase',    'activityservice',     'provisioningservice',
		'reportingservice', 'testerAdminPassword', 'psTesterPassword',
		'dataSbmtService',  'vzPublisherService',  'vzProxyService',
		'awsEsUrl'
	];

	function validateProperty(property_name) {
		var res = config.hasOwnProperty(property_name) &&
			(typeof config[property_name] === 'string') &&
			config[property_name];
		if (!res)
			console.error("'%s' parameter missed in the config or has invalid type or value.", property_name);
		return res;
	}

	var res = false;
	if (typeof config === 'object') {
		res = true;
		config_properties.forEach(function (cfg_property) {
			// using tmp_res to disable optimization and loop over all properties
			var tmp_res = validateProperty(cfg_property);
			res = res && tmp_res;
		});
	}
	return res
}

function setConfigValues(config){
	exports.environment = config.environment;
	exports.origin = config.origin;
	exports.oauthDomain = config.oauthDomain;
	exports.userservice = config.userservice;
	exports.userdataService = config.userdataService;
	exports.projectservice = config.projectservice;
	exports.stashRepoBase = config.stashRepoBase;
	exports.activityservice = config.activityservice;
	exports.provisioningservice = config.provisioningservice;
	exports.reportingservice = config.reportingservice;
	exports.testerAdminPassword = config.testerAdminPassword;
    exports.psTesterPassword = config.psTesterPassword;
	exports.dataSbmtService = config.dataSbmtService;
	exports.vzPublisherService = config.vzPublisherService;
	exports.vzProxyService = config.vzProxyService;
    exports.awsEsUrl = config.awsEsUrl;
}

exports.setConfig = function(environment_name){
	var result = false;
	if (configJson.hasOwnProperty(environment_name)){
		var config = configJson[environment_name];
		config.environment = environment_name;
		result = checkConfig(config);
		if (result) {
			config.environment = environment_name;
			setConfigValues(config);
			console.log("Configuration switched to '%s'",environment_name);
		}
	}
	return result;
};
