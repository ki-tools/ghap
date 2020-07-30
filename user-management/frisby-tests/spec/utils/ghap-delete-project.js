/**
 * Created by Vlad on 23.05.2015.
 */
var ctrlFlow = require('../ums/control_flow');

var prjSrv = require('./ghap-project-service');
var testerAdmin = require('../ums/tester_admin');
var oAuth = require('./ghap-oauth');

var allProjects = [];

var projectKey = null;
if (typeof process.argv[2] === 'string'){
	projectKey = process.argv[2];
}
if (projectKey !== null) {
	oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
		.then(runCommand);
} else {
	console.log("No project name specified.");
}

function runCommand() {
	prjSrv.getAllProjects(oAuth.header, allProjects, function(result){
		if (!result.is_successful) {
			console.log('Delete failed due invalid response on gatAllProjects request.');
			return
		}

		if (projectKey.toLowerCase() === '--all')
			deleteAllProjects();
		else
			deleteProjectByKey(projectKey);

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
