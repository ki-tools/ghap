var
	fs = require('fs'),
	path = require('path'),
	util = require('util'),
	cp = require('child_process');

var log4js = require('log4js');
log4js.configure({
	appenders: [
		{ type: 'console', category: 'stdout' },
		{ type: 'file', filename: 'ghap-import-data.log', category: ['stdout','filelog'] }
	]
});
var logger = log4js.getLogger('stdout');
var fileLogger = log4js.getLogger('filelog');

var ctrlFlow = require('../ums/control_flow');

var prjSrv = require('./ghap-project-service');
var prjRes = require('../prj-prov/prj-prov_resources');
var testerAdmin = require('../ums/tester_admin');
var oAuth = require('./ghap-oauth');

function getDirectories(srcpath) {
	try {
		return fs.readdirSync(srcpath).filter(function (file) {
			return fs.statSync(path.join(srcpath, file)).isDirectory();
		});
	} catch (ex) {
		console.error(ex.message);
		return [];
	}
}

var data_path = '.';
if (typeof process.argv[2] === 'string')
	data_path = process.argv[2];

var base_path = path.resolve('.');
if (!path.isAbsolute(data_path))
	data_path = path.join(base_path, data_path);

var data_dirs = getDirectories(data_path);
if (data_dirs.length > 0) {
	oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
		.then(start);
} else {
	logger.warn("No directories found on path '%s'",data_path);
}

var allProjects = [];
var handledProjects = [];

function start(){
	prjSrv.getAllProjects(oAuth.header, allProjects, function(result){
		if (result.is_successful) {
			importProjects();
		} else{
			fileLogger.info(result.message);
			logger.error('Project service is unavailable. Script cancelled.');
			final();
		}
	})
}

function final(){
	var delete_calls = [];
	//handledProjects.forEach(function(prj_res){
	//	delete_calls.push(
	//		function (next) {	prjSrv.deleteProject(oAuth.header,prj_res,next); }
	//	);
	//});

	ctrlFlow.series(delete_calls, function(){
		log4js.shutdown(function(){process.exit(0)});
	});
}

function importProjects(){

	var prj_dir_name = data_dirs.shift();
	if (prj_dir_name)
		importProject(prj_dir_name, importProjects);
	else
		final();
}

function importProject(prj_dir_name, callback){
	var prj_path = path.join(data_path, prj_dir_name);

	var prj_key = getProjectKey(prj_dir_name);
	var prj_res = prjRes.findProjectByKey(allProjects, prj_key);
	if (prj_res !== null) {
		logger.info("Project '%s' with key '%s' already exists. Will try to import grants.",prj_res.name, prj_res.key);
		prjSrv.getAllGrants(oAuth.header,prj_res, function(result){
			if (result.is_successful) {
				fileLogger.info(result.message);
				handledProjects.push(prj_res);
				importGrants(prj_res, prj_path, callback)
			} else {
				fileLogger.error(result.message);
				final();
			}
		})

	} else {
		prj_res = prjRes.makeProject(
			getProjectName(prj_dir_name),
			getProjectKey(prj_dir_name),
			getProjectDescription(prj_dir_name)
		);
		prjSrv.createProject(oAuth.header, prj_res, function (create_result) {
			fileLogger.info(create_result.message);
			if (create_result.is_successful) {
				handledProjects.push(prj_res);
				importGrants(prj_res, prj_path, callback)
			} else {
				logger.warn('Import of grants cancelled due error while creating the project.');
				callback();
			}
		});
	}
}

function getProjectName(prj_dir_name){
	return prj_dir_name
}

function getProjectKey(prj_dir_name) {
	return prj_dir_name.toUpperCase()
}

function getProjectDescription(prj_dir_name) {
	return prj_dir_name
}

function importGrants(prj_res, prj_path, calback) {
	var grants_dirs = getDirectories(prj_path);
	var importGrant_calls = [];
	grants_dirs.forEach(function(gran_dir_name){
		importGrant_calls.push(
			function (next) {	importGrant(prj_res, prj_path, gran_dir_name, next); }
		);
	});
	ctrlFlow.series(importGrant_calls, calback)
}

function importGrant(prj_res, prj_path, grant_dir_name, callback) {
	var grant_res = prjRes.makeGrant(grant_dir_name);

	if( prjRes.findGrantByName(prj_res.grants, grant_res.name) !== null) {
		logger.warn("Grant '%s' already exists in program '%s'. Pushing of the data will be skipped.",
			grant_res.name, prj_res.name);
		callback();
		return
	}

	prjSrv.createGrant(oAuth.header, prj_res, grant_res, function(create_result){
		fileLogger.info(create_result.message);
		if (create_result.is_successful) {
			var grant_path = path.join(prj_path, grant_dir_name);
			pushToStash(grant_path, prj_res.key, grant_res.name, callback);
		}	else {
			logger.warn('Pushing to stash cancelled due error while creating the grant.');
			callback();
		}
	})
}

function pushToStash(grant_path, prj_key, grant_name, callback) {

	logger.info('chdir '+grant_path);
	process.chdir(grant_path);

	// https://strongloop.com/strongblog/node-js-v0-12-shell-programming-synchronous-child-process/
	if (fs.existsSync('.git'))
		runCommand('rmdir /s /q .git');
	runCommand('git init');
	runCommand('git add --all');
	var log_file = path.join(base_path, 'git-commit.log');
	runCommand('git commit -m "Initial Commit" >> '+log_file);
	runCommand('git remote add origin https://Administrator@git.dev.ghap.io/stash/scm/'
	  + prj_key.toLowerCase() +'/'+ grant_name.toLowerCase() +'.git');
	runCommand('git push origin master');

	callback();
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