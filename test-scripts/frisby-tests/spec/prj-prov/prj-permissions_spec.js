/**
 * Created by vruzov on 17.09.2015.
 */

var Q = require('q');
var util = require('util');
var i;

var my = require('../Common/ghap-lib');
my.stepPrefix = 'PrjPermissions';
my.logModuleName(module.filename);

var allProjects = [];
var prjResources = require('./prj-prov_resources');
var prjRequests = require('./prj-requests');
var testProjects = [];
for(i = 0; i < 2; i++)
    testProjects.push( prjResources.makeProject('TstPrj'+i, 'TstPrj'+i+'Key', 'Test Project '+i+' description') )

var testGrant = prjResources.makeGrant('Grant1');

var umsUser = require('../ums/ums_user');
var umsRequests = require('../ums/ums_requests');
var autoTesters = [];
for(i = 0; i < 5; i++)
    autoTesters.push(umsUser.makeUser('Tester'+i, 'Auto'))

var umsGroup = require('../ums/ums_group');
var groupCRUD = require('../ums/ums_group_crud');
var testersGroup = umsGroup.create( autoTesters[0].getParentDn(), 'AutoTesters', 'AutoTesters group');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(umsRequests.validateToken)
    .then(runSuite)
    .catch(my.reportError)
    .finally(finalCase);

function runSuite() {

    return groupCRUD.pullGroup(oAuth.header, testersGroup)
      .then(createTestersGroupIfNotExists)
      .then(pullTesters)
      .then(createTestersIfNotExists)
      .then(getTestersGroupMembers)
      .then(addTestersToGroup)
      .then(getAllProjects)
      .then(createTestProjectsIfNotExist)
      .then(createGrantsIfNotExist_series)
      .then(grantPermissions4users)
      .then(validateProjectPermissions4users)
      .then(validateGrantsPermissions4user)
      //.then(grantPermissionsOnProject4group)
      .then(deleteTestProjects)
      .then(deleteAutoTesters)
}

function createTestersGroupIfNotExists(){
    if (testersGroup.guid) return;
    return groupCRUD.createGroup(oAuth.header, testersGroup)
}

function pullTesters(){
    // Q: q promise one by another
    // A: http://stackoverflow.com/questions/24586110/resolve-promises-one-after-another-i-e-in-sequence
    var p = Q();
    autoTesters.forEach(function(autoTester) {
        p = p.then(function(){return umsRequests.pullUserData(oAuth.header, autoTester)})
    });
    return p;
}

function createTestersIfNotExists(){
    var p = Q();
    autoTesters.forEach(function(autoTester) {
        p = p.then(function(){
            if (autoTester.getGuid() === 'unknown')
                return umsRequests.createUser(oAuth.header, autoTester)
        })
    });
    return p;
}

function addTestersToGroup() {
    var promises = [];
    for(var i = 0; i < autoTesters.length; i++) {
        if (!my.findElementInArray(testersGroup.members,'guid', autoTesters[i].getGuid()))
            promises.push( groupCRUD.addMemberToGroup(oAuth.header, autoTesters[i], testersGroup) );
    }
    if (promises.length === 0)
        console.log("\nAll AutoTesters are members of '%s' group.", testersGroup.name);

    return Q.allSettled(promises)
}

function getTestersGroupMembers(){
    return groupCRUD.getGroupMembers(oAuth.header, testersGroup)
}

function getAllProjects(){
    return prjRequests.getAllProjects(oAuth.header, allProjects)
}

function createTestProjectsIfNotExist(){
    var p = Q();
    testProjects.forEach(function(testProject) {
        p = p.then(function(){
            var prj = my.findElementInArray(allProjects, 'name', testProject.name);
            if (prj == null) {
                return prjRequests.createProject(oAuth.header, testProject )
            } else {
                my.copyProperties(prj,testProject);
                console.log("'%s' project exists.", testProject.name);
            }
        })
    });

    return p;
}

function createGrantsIfNotExist() {
    var promises = [];

    console.log("\nStart creating of grants in test projects if they are no exist.");

    var testGrants = [];
    for (var i = 0; i < 3; i++)
        testGrants.push(prjResources.makeGrant("G"+i));

    for(i = 0; i < testProjects.length; i++)
        promises.push( prjRequests.getAllGrants(oAuth.header, testProjects[i]) )

    return Q.allSettled(promises)
      .then(function(){
          var create_grants_promises = [];
          for(var i = 0; i < testProjects.length; i++)
              for (var j=0; j < testGrants.length; j++) {
                  if (!my.findElementInArray(testProjects[i].grants,'name',testGrants[j].name)) {
                      create_grants_promises.push(
                        prjRequests.createGrant(oAuth.header, testProjects[i], testGrants[j])
                      )
                  }
              }
          if (create_grants_promises.length === 0)
              console.log("\nAll test projects have all required grants.");
          return Q.allSettled(create_grants_promises);
      })
      .catch(my.reportError)
}

function createGrantsIfNotExist_series() {
    var promises = [];

    console.log("\nStart creating of grants in test projects if they are no exist.");

    var testGrants = [];
    for (var i = 0; i < 3; i++)
        testGrants.push(prjResources.makeGrant("G"+i));

    var p = Q();
    testProjects.forEach(function(testProject){
        p = p.then(function(){return prjRequests.getAllGrants(oAuth.header, testProject)})
    });

    testProjects.forEach(function(testProject){
        p = p.then(function(){
            var p = Q();
            testGrants.forEach(function(testGrant){
                p = p.then(function() {
                    if (!my.findElementInArray(testProject.grants, 'name', testGrant.name)) {
                        return prjRequests.createGrant(oAuth.header, testProject, testGrant)
                    }
                })
            });
            return p;
        })
    });

    return p.catch(my.reportError)
}


function grantPermissions4users() {
    var promises = [];
    promises.push( prjRequests.revokeProjectPermissions(oAuth.header, testProjects[0], autoTesters[0], ["READ","WRITE"]) );
    promises.push( prjRequests.revokeProjectPermissions(oAuth.header, testProjects[0], autoTesters[1], ["READ","WRITE"]) );
    promises.push( prjRequests.revokeProjectPermissions(oAuth.header, testProjects[0], autoTesters[2], ["READ","WRITE"]) );

    for (var i=0; i< testProjects[1].grants.length; i++)
        promises.push( prjRequests.revokeGrantPermissions(oAuth.header, testProjects[1].grants[i], autoTesters[0], ["READ","WRITE"]) );

    return Q.allSettled(promises)
      .then(function(){
          var promises = [];
          promises.push( prjRequests.grantPermissionsOnProject(oAuth.header, testProjects[0], autoTesters[0], ["READ"]) );
          promises.push( prjRequests.grantPermissionsOnProject(oAuth.header, testProjects[0], autoTesters[1], ["READ","WRITE"]) );
          promises.push( prjRequests.grantPermissionsOnGrant(oAuth.header, testProjects[1].grants[1], autoTesters[0], ["READ"]) );
          promises.push( prjRequests.grantPermissionsOnGrant(oAuth.header, testProjects[1].grants[2], autoTesters[0], ["READ","WRITE"]) );
          return Q.allSettled(promises);
      })
}

function validateProjectPermissions4users(){
    var promises = [];
    promises.push( prjRequests.getAllProjects4User(oAuth.header, autoTesters[0]) );
    promises.push( prjRequests.getAllProjects4User(oAuth.header, autoTesters[1]) );
    promises.push( prjRequests.getAllProjects4User(oAuth.header, autoTesters[2], null, function(response_status){
        if (response_status === 404) {
            console.log("Response status code has expected value 404.");
            return true;
        }
    }) );

    return Q.allSettled(promises)
        .then( function(){
            var deferred = Q.defer();
            console.log('.');
            var msg = util.format("Validate permissions for project '%s'", testProjects[0].name );
            describe(msg, function(){

                msg = util.format("- %s should have only READ permission", autoTesters[0].getName());
                it(msg, function(){
                    console.log(this.getFullName());
                    var prj = my.findElementInArray(autoTesters[0].projects, 'name', testProjects[0].name);
                    expect(prj).not.toBeNull();
                    expect(my.findElementInArray(prj.permissions, 'READ')).toBe('READ');
                    expect(my.findElementInArray(prj.permissions, 'WRITE')).toBeNull();
                });

                msg = util.format("- %s should have READ,WRITE permission", autoTesters[1].getName());
                it(msg, function(){
                    console.log(this.getFullName());
                    var prj = my.findElementInArray(autoTesters[1].projects, 'name', testProjects[0].name);
                    expect(prj).not.toBeNull();
                    expect(my.findElementInArray(prj.permissions, 'READ')).toBe('READ');
                    expect(my.findElementInArray(prj.permissions, 'WRITE')).toBe('WRITE');
                });

                msg = util.format("- %s should have no this project", autoTesters[2].getName());
                it(msg, function(){
                    console.log(this.getFullName());
                    var prj = my.findElementInArray(autoTesters[2].projects, 'name', testProjects[0].name);
                    expect(prj).toBeNull();
                });

                it("- all specs were executed.", function(){
                    deferred.resolve();
                    waits(300);  // to allow promise to be fulfilled and start next test
                })
            });
            return deferred.promise;
        })
}

function validateGrantsPermissions4user(){
    return prjRequests.getAllGrants(oAuth.header, testProjects[1])
      .then(function(){
          var deferred = Q.defer();
          console.log('.');
          var msg = util.format("Validate grants permissions for project '%s'", testProjects[1].name );
          describe(msg, function(){

              msg = util.format("- project should have no less than 3 grants");
              it(msg, function(){
                  console.log(this.getFullName());
                  expect(testProjects[1].grants.length).toBeGreaterThan(2)
              });

              it("- all specs were executed.", function(){
                  deferred.resolve();
                  waits(300);
              })
          });
          return deferred.promise;
      })
      .catch(my.reportError)
}

function grantPermissionsOnProject4group() {
    return prjRequests.grantPermissionsOnProject(oAuth.header, testProjects[0], testersGroup, ["READ"])
}

function deleteTestProjects(){
    var p = Q();
    testProjects.forEach(function(tstProject) {
        p = p.then(function(){return prjRequests.deleteProject(oAuth.header, tstProject )})
    });
    return p;
}

function deleteAutoTesters(){
    var p = Q();
    autoTesters.forEach(function(autoTester) {
        p = p.then(function(){return umsRequests.deleteUser(oAuth.header, autoTester )})
    });
    return p;
}

function finalCase(){
    console.log('\nTest case finished.');
}
