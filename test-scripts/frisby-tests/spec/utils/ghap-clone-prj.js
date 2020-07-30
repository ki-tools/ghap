var
    fs = require('fs'),
    path = require('path'),
    util = require('util'),
    cp = require('child_process');

var cfg = require('./../Common/ghap-config');

var log4js = require('log4js');
log4js.configure({
    appenders: [
        { type: 'console', category: 'stdout' },
        { type: 'file', filename: 'ghap-clone-prj.log', category: ['stdout','filelog'] }
    ]
});
var logger = log4js.getLogger('stdout');
var fileLogger = log4js.getLogger('filelog');

var ctrlFlow = require('../Common/control_flow');

var args = process.argv.slice(2);
if (args.length !== 2){
    console.log("Use: node %s <PROJECT_KEY> path", process.argv[1]);
    process.exit(1);
}

var arg_num = 0;
var projectKey = args[arg_num++];
var dataPath = args[arg_num++];
var basePath = path.resolve('.');
if (!path.isAbsolute(dataPath)) dataPath = path.join(basePath, dataPath);

makeDataDir(dataPath);
logger.info('chdir '+dataPath);
process.chdir(dataPath);

var prjSrv = require('./ghap-project-service');
var prjRes = require('../prj-prov/prj-prov_resources');
var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./ghap-oauth');
var oAuth = oAuthService.makeOAuthClient();

oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(start);

function makeDataDir(data_path) {
    var path_exists;
    try {
        var stats = fs.lstatSync(data_path);
        path_exists = true;
    } catch (e) {
        path_exists = false;
    }

    if (path_exists) {
        logger.error("'%s' path already exists.", data_path);
        final();
    } else {
        fs.mkdirSync(data_path);
    }
}

var allProjects = [];
var handledProjects = [];

function start(){
    prjSrv.getAllProjects(oAuth.header, allProjects, function(result){
        fileLogger.info(result.message);
        if (result.is_successful) {
            var prj = prjRes.findProjectByKey(allProjects, projectKey);
            if (prj) {
                var prj_res = prjRes.makeProject(prj.name, prj.key, prj.description);
                prj_res.id = prj.id;
                cloneGrants(prj_res);
            } else {
                fileLogger.info("Project with key '"+projectKey+"' not found.");
                logger.error("Project with key '%s' not found.", projectKey);
                final();
            }
        } else{
            logger.error('Get All projects request failed. Script cancelled.');
            final();
        }
    })
}

function final(){
    log4js.shutdown(function(){process.exit(0)});
}

function cloneGrants(prj_res){
    prjSrv.getAllGrants(oAuth.header, prj_res, function(result){
        fileLogger.info(result.message);
        if (result.is_successful) {
            logger.info("Project '%s' have %d grants", prj_res.name, prj_res.grants.length);
            prj_res.grants.forEach(function(grant) {
                cloneGrant(prj_res, grant)
            })
        } else{
            logger.error('Get All grants request failed. Script cancelled.');
        }
        final();
    })
}

function cloneGrant(prj, grant) {
    runCommand('git clone '+ cfg.stashRepoBase + '/' + prj.key.toLowerCase()
        +'/'+ grant.name.toLowerCase() +'.git ' + grant.name);
    runCommand('rmdir '+ grant.name + '\\.git /s /q');
}

function runCommand(cmd_str){
    logger.info(cmd_str);
    try {
        var res = cp.execSync(cmd_str + ' 2>&1');
        if (res.length > 0)
            logger.info(res.toString())
    } catch (error) {
        logger.error(error.message);
    }
}