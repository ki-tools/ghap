/**
 * Created by Vlad on 23.05.2015.
 */
var ctrlFlow = require('../Common/control_flow');

var cfg = require('./../Common/ghap-config');

var optimist = require('optimist').
usage('Usage: node ghap-delete-project <PROJECT-KEY> --env <ENVIRONMENT>\n' +
		'delete GHAP project specified by PROJECT-KEY').
describe('env', "environment where project maintained (the possible values are 'prod', 'qa', 'samba')")

var argv = optimist.argv;

if (argv.env) {
	if (argv.env !== cfg.environment)
		if (!cfg.setConfig(argv.env)) {
			console.error("Invalid environment name '%s'", argv.env);
			process.exit(1);
		}
} else {
	optimist.showHelp();
	process.exit(1);
}

var prjSrv = require('./ghap-project-service');
var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./ghap-oauth');
var oAuth = oAuthService.makeOAuthClient();

var allProjects = [];

oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
		.then(runCommand);

function runCommand() {

	prjSrv.getAllProjects(oAuth.header, allProjects, function(result){
		if (!result.is_successful) {
			console.log('Invalid response on gatAllProjects request. Delete fails.');
			return
		}

		if (!allProjects.length) {
			console.log("")
		}

		if (argv.deleteAllProjects) {
			if (argv.deleteAllProjects === 'on'+cfg.environment.toUpperCase()) {
				console.log("Delete all projects on '%s' environment.", cfg.environment);
				if (cfg.environment.toUpperCase() === 'PROD') {
					console.log('Permissions denied.');
				} else {
					deleteAllProjects();
				}
			} else {
				console.log("Invalid value of deleteAllProjects key.")
			}
		} else {
			var projectKey = argv._[0];
			if (!projectKey) {
				console.error("Project key not specified.")
				optimist.showHelp();
				process.exit(1);
			}
			deleteProjectByKey(projectKey);
		}
	})
}

function deleteAllProjects(){
	var delete_calls = [];
	allProjects.forEach(function(prj_res){
		delete_calls.push(
			function (next) {
				prjSrv.deleteProject(oAuth.header, prj_res, next);
			}
		);
	});
	ctrlFlow.series(delete_calls, function(){});
}

function deleteProjectByKey(project_key){
	var prj_res = findProjectByKey(allProjects, project_key);
	if (prj_res === null){
		console.log("Project with key '%s' not found.",project_key);
		return
	}
	prjSrv.deleteProject(oAuth.header, prj_res, function(result){
		if (result.is_successful)
			console.log('Successful.');
		else
			console.log('Failed.');
	})
}

function findProjectByKey(all_projects, project_key){
	var filtered_projects = all_projects.filter(function (prj_res) {
		return (prj_res.key == project_key)
	});
	if (filtered_projects.length === 1)
		return filtered_projects[0];
	return null;
}