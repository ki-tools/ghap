/**
 * Created by Vlad on 25.05.2015.
 */

var ctrlFlow = require('../Common/control_flow');

var prjRes = require('../prj-prov/prj-prov_resources');
var prjSrv = require('./ghap-project-service');
var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./ghap-oauth');
var oAuth = oAuthService.makeOAuthClient();

var allProjects = [];

var projectKey = null;
if (typeof process.argv[2] === 'string'){
	projectKey = process.argv[2];
}
var grantName = null;
if (typeof process.argv[3] === 'string'){
	grantName = process.argv[3];
}

if (projectKey === null || grantName === null) {
	console.info('Use: ghap-delete-grant <PROJECT_KEY> <GRANT_NAME>')
}
else {
	oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
		.then(runCommand);
}

function runCommand() {
	prjSrv.getAllProjects(oAuth.header, allProjects, function(result){
		if (!result.is_successful) {
			console.log('Delete failed due invalid response on gatAllProjects request.');
			return
		}
		var prj_res = prjRes.findProjectByKey(allProjects,projectKey);
		if (prj_res == null) {
			console.error("Project with key '%s' not found", projectKey);
			return
		}
		prjSrv.getAllGrants(oAuth.header, prj_res, function(result){
			if (!result.is_successful) {
				console.log("Can not get grants for program '%s'.", prj_res.name);
				return
			}
			var grant_res = prjRes.findGrantByName(prj_res.grants, grantName);
			if (grant_res === null) {
				console.log("Grant '%s' not found in program '%s'.", grantName, prj_res.name);
				return
			}
			prjSrv.deleteGrant(oAuth.header,grant_res, function(result){
				if (result.is_successful)
					console.log('Successful.');
				else
					console.log('Failed.');
			})
		})

	})
}
