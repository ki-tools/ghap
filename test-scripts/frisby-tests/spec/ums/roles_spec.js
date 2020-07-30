/**
 * Created by Vlad on 06.08.2015.
 */

var my = require('../Common/ghap-lib');
my.stepPrefix = 'RolesCRUD';
my.logModuleName(module.filename);

var util = require('util');
var ctrlFlow = require('../Common/control_flow');

var umsRole = require('../ums/ums_role');
var roleCRUD = require('../ums/ums_role_crud');

var allRoles = [];
var dataAnalystRole;
var dataCuratorRole;
var dataContributorRole;

var testerRole = umsRole.makeRole('GHAP Tester Role', 'Tester role.');

var umsUser = require('./ums_user');
var umsRequests = require('./ums_requests');
var Tester = umsUser.makeUser('RoleTester');

var asRequests = require('../as/as_requests');
var asActivity = require('../as/as_activity');
var testActivity = asActivity.makeActivity('Testing & Analysis. Test activity.');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_service2');
oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
	.then(runSuite);

function runSuite(){
	roleCRUD.getAllRoles(oAuth.header, allRoles, function(err_count){
		if (err_count === 0){
			dataAnalystRole = umsRole.findRoleByName(allRoles, 'Data Analyst');
			expect(dataAnalystRole).not.toBeNull();
			dataCuratorRole = umsRole.findRoleByName(allRoles, 'Data Curator');
			expect(dataCuratorRole).not.toBeNull();
			dataContributorRole = umsRole.findRoleByName(allRoles, 'Data Contributor');
			expect(dataContributorRole).not.toBeNull();
		}
		if (dataAnalystRole && dataCuratorRole && dataContributorRole)
			runRole_tests();
		else
			final();
	});
}

function runRole_tests(){
	var testerRoleIsDeleted = false;
	ctrlFlow.series([
		function (next) {roleCRUD.createRole(oAuth.header, testerRole, next);},
		function (next) {roleCRUD.getRole(oAuth.header, testerRole, next);},
		function(next) {asRequests.createActivity(oAuth.header, testActivity,
			function (err_count) {
				var calls = [];
				if (err_count == 0){
					calls.push(
						function(next) {asRequests.createActivityRoleAssociation(oAuth.header, testActivity, testerRole, next );},
						function(next) {asRequests.getARAssociationsForRole(oAuth.header, testerRole, next);},
						function(next) {
							describe('Validate AR association:', function(){
								var test_description = util.format("'%s' should be associated with '%s'", testActivity.activityName, testerRole.name);
								it(test_description, function(){
									console.log(this.getFullName());
									expect(testerRole.ar_associations.length).toEqual(1);
									expect(testerRole.ar_associations[0].id).toBeDefined();
									expect(testerRole.ar_associations[0].roleId).toEqual(testerRole.guid);
									expect(testerRole.ar_associations[0].activityId).toEqual(testActivity.id);
									next();
								})
							})
						}
					)
				} else
					calls.push(	function(next) {asRequests.getActivityByName( oAuth.header, testActivity.activityName, testActivity, next ); } );

				calls.push(	function(next) {asRequests.deleteActivity( oAuth.header, testActivity, next ); } );
				ctrlFlow.series(calls, next)
			}
		);},
		function (next) {umsRequests.createUser(oAuth.header, Tester,
			function (err_count) {
				if (err_count == 0) {
					ctrlFlow.series([
						function (next) {roleCRUD.setRoleToUser(oAuth.header, Tester, testerRole, next);},
						function (next) {roleCRUD.setRoleToUser(oAuth.header, Tester, dataAnalystRole, next);},
						function (next) {umsRequests.getUserRoles(oAuth.header, Tester,
							function(){
								var num_roles = Tester.getRoles().length;
								expect(num_roles).toBe(2);
								if (num_roles !== 2) console.error("Expected that Tester have 2 roles.");
								next()
							});
						},
						function (next) {roleCRUD.deleteRole(oAuth.header, testerRole,
							function(err_count){
								if (err_count ===0 ) testerRoleIsDeleted = true;
								next()
	 					  });
						},
						function (next) {umsRequests.getUserRoles(oAuth.header, Tester,
							function(){
							  var num_roles = Tester.getRoles().length;
							  expect(num_roles).toBe(1);
							  if (num_roles !== 1) console.error("Expected that Tester have 1 role.");
							  next()
						  });
						},
						function (next) {asRequests.getARAssociationsForRole(oAuth.header, dataAnalystRole,
							function(){
								var num_AR_associations = dataAnalystRole.ar_associations.length;
								expect(num_AR_associations).toBeGreaterThan(2);
								if (num_AR_associations < 3) console.error("Expected that Data Analyst have 3 or more activities.");
								next()
							});
						},
						function (next) {asRequests.getARAssociationsForRole(oAuth.header, dataCuratorRole,
							function(){
								var num_AR_associations = dataCuratorRole.ar_associations.length;
								expect(num_AR_associations).toBeGreaterThan(2);
								if (num_AR_associations < 3) console.error("Expected that Data Curator have 3 or more activities.");
								next()
							});
						}
					], next)
				} else
					next();
			});
		},
		function (next) {umsRequests.deleteUser(oAuth.header, Tester, next);},
		function (next) {if (testerRoleIsDeleted) next(); else roleCRUD.deleteRole(oAuth.header, testerRole, next);}
	], final);
}

function final(){
	console.log("\nScript '%s' finished.", my.getModuleName(module.filename))
}