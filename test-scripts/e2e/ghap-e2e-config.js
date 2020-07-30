/**
 * Created by vruzov on 12.10.2015.
 */

var configJson = require('./ghap-e2e-config.json');
var ghapE2EIni = require('./ghap-e2e-ini');

var environment_name = configJson.defaultEnvironment;
if (process.env.GHAP_ENV) environment_name = process.env.GHAP_ENV;
console.log("Configured default environment is '%s'.", environment_name);

if (global.hasOwnProperty('browser') && browser.params.hasOwnProperty('environment')) {
    environment_name = browser.params['environment'].toLowerCase();
    console.log("Environment will be changed to '%s'", environment_name);
}

if (!configJson.hasOwnProperty(environment_name)) {
    console.log("ERROR: Invalid environment '%s'", environment_name);
    browser.close()
        .then(function(){ process.exit(1); });
    return;
}

var config = configJson[environment_name];
config.environment = environment_name;
var common_config = configJson['common'];

join_configs(config, common_config);

if (!checkConfig(config)) {
    console.log('ERROR: Invalid configuration file.');
    process.exit(1);
}

var e2eIni = ghapE2EIni.get(config.environment);
if (e2eIni.user.password) {
    config.userPassword = e2eIni.user.password;
}

setExportsValues(config);

/*------------------------ End Of executable part -------------------------------*/

function checkConfig(config){
    return (typeof config === 'object') &&
        config.hasOwnProperty("ghapUrl") && (typeof config.ghapUrl === 'string') &&
        config.hasOwnProperty("adminName") && (typeof config.adminName === 'string') &&
        config.hasOwnProperty("adminPassword") && (typeof config.adminPassword === 'string')
}

function join_configs(config, common_config) {
    for ( var key in common_config ) {
        if (!config.hasOwnProperty(key)) {
            config[key] = common_config[key]
        }
    }
}

function renewUserPassword(new_password) {
    var e2e_ini = ghapE2EIni.get(exports.environment);
    e2e_ini.user.password = new_password;
    ghapE2EIni.save(e2e_ini, exports.environment);
    exports.userPassword = new_password;
}

function setExportsValues(config){
    exports.environment = config.environment;
    exports.ghapUrl = config.ghapUrl;
    exports.adminName = config.adminName;
    exports.adminPassword = config.adminPassword;
    exports.userName = config.userName;
    exports.userNamePattern = new RegExp(config.userNamePattern);
    exports.userPassword = config.userPassword;
    exports.userEmail = config.userEmail;
    exports.emailPassword = config.emailPassword;
    exports.mailServer = config.mailServer;
    exports.mailServerPort = config.mailServerPort;
    exports.contributorName = config.contributorName;
    exports.contributorPassword = config.contributorPassword;
    exports.curatorName = config.curatorName;
    exports.curatorPassword = config.curatorPassword;
    exports.tstFileName = config.tstFileName;
    exports.tstFileSizeMb = config.tstFileSizeMb;
    exports.renewUserPassword = renewUserPassword;
}