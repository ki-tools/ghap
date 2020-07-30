/**
 * Created by vruzov on 17.09.2015.
 */

var Q = require('q');

var my = require('./../Common/ghap-lib');
my.stepPrefix = 'SetupAutoTesters';
my.logModuleName(module.filename);

var umsUser = require('./ums_user');
var umsRequests = require('./ums_requests');

var autoTesters = [];
for(var i = 0; i < 5; i++)
    autoTesters.push(umsUser.makeUser('Tester'+i, 'Auto'))

var umsGroup = require('./ums_group');
var groupCRUD = require('./ums_group_crud');

var testersGroup = umsGroup.create( autoTesters[0].getParentDn(), 'AutoTesters', 'AutoTesters group');

var testerAdmin = require('./tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(umsRequests.validateToken)
    .then(runSuite)
    .catch(my.reportError)
    .finally(finalCase);

function runSuite(){
    return groupCRUD.pullGroup(oAuth.header, testersGroup)
        .then(createTestersGroupIfNotExists)
        .then(pullTesters)
        .then(createTestersIfNotExists)
        .then(getTestersGroupMembers)
        .then(addTestersToGroup)
}

function createTestersGroupIfNotExists(){
    if (testersGroup.guid) return;
   return groupCRUD.createGroup(oAuth.header, testersGroup)
}

function pullTesters(){
    var promises = [];
    for(i = 0; i < autoTesters.length; i++) {
        promises.push( umsRequests.pullUserData(oAuth.header, autoTesters[i]) );
    }
    return Q.allSettled(promises);
}

function createTestersIfNotExists(){
    var promises = [];
    for(i = 0; i < autoTesters.length; i++) {
        if (autoTesters[i].getGuid() === 'unknown')
            promises.push( umsRequests.createUser(oAuth.header, autoTesters[i]) );
    }
    return Q.allSettled(promises);
}

function addTestersToGroup() {
    var promises = [];
    for(i = 0; i < autoTesters.length; i++) {
        if (!my.findElementInArray(testersGroup.members,'guid', autoTesters[i].getGuid()))
            promises.push( groupCRUD.addMemberToGroup(oAuth.header, autoTesters[i], testersGroup) );
    }
    if (promises.length === 0)
    console.log("\nAll AutoTesters are members of '%s' group.", testersGroup.name);
    return Q.allSettled(promises);
}

function getTestersGroupMembers(){
    return groupCRUD.getGroupMembers(oAuth.header, testersGroup)
}

function finalCase(){
    console.log('\nTest case finished.');
}