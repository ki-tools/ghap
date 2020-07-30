/**
 * Created by Vlad on 02.02.2016.
 */

var Q = require('q');

var my = require('../Common/ghap-lib');
my.stepPrefix = 'ListStacks';
my.logModuleName(module.filename);

var asRequests = require('../as/as_requests');
var psRequests = require('../ps/ps_requests');
var umsRequests = require('../ums/ums_requests');

var testerAdmin = require('../ums/tester_admin');
var oAuthService = require('./../oauth/oauth_promise');
var oAuth = oAuthService.makeOAuthClient();
oAuth.login(testerAdmin.getName(), testerAdmin.getPassword())
    .then(function() {
        return umsRequests.pullUser(oAuth, testerAdmin)
            .then(function () {
                console.log(" user GUID '%s'", testerAdmin.getGuid())
            })
    })
    .then(runSuite)
    .catch(my.reportError)
    .finally(my.finalTest);

/**
 * All Ghap Users
 * @type {ghapUser[]}
 */
var allGhapUsers = [];
/**
 * All Ghap Activities
 * @type {ghapActivity[]}
 */
var allGhapActivities = [];
/**
 * All AWS instances provisioned to GHAP users
 * @type {awsUserInstance[]}
 */
var allAwsUserInstances =[];
/**
 * All compute resources registered in the provisioning service
 * @type {computeResource[]}
 */
var allComputeResources = [];
/**
 * All stacks registered in the provisioning service
 * @type {vpgStack[]}
 */
var allStacks = [];

function runSuite() {
    return umsRequests.getAllUsers(oAuth.header, testerAdmin.getParentDn(), allGhapUsers)
        .then(asRequests.getAllActivities.bind(this,oAuth.header, allGhapActivities))
        .then(psRequests.multiVpgGetAllStacks.bind(this, oAuth.header, allStacks))
        .then(psRequests.multiVpgGetAllAwsInstances.bind(this,oAuth.header, allAwsUserInstances))
        .then(listAwsInstances)
        .then(psRequests.multiVpgGetAllComputeResources.bind(this,oAuth.header, allComputeResources))
        .then(listComputeResources)
        .then(getComputeResources4AllStacks)
        .then(listStacks)
        //.then(listStackDetails4User.bind(this,'6ea0c720-c21d-4f5b-9171-95a28208d2a3'))
}

function getComputeResources4AllStacks() {
    var p = Q();
    allStacks.forEach(function(stack) {
        p = p.then(function () {
            return psRequests.multiVpgGetComputeResources4Stack(oAuth.header, stack)
        })
    });
    return p;
}

function listStacks() {

    allStacks.forEach(function (stack) {

        console.log("\nID: %s", stack.id);
        console.log("User: %s", getUserNameById(stack.userId));
        console.log("Activity: %s", getActivityNameById(stack.activityId));
        console.log('Compute resources: %d', stack.computeResources.length)
        stack.computeResources.forEach(function (compute_resource) {
            var ip_str = my.pad(4 * 3 + 4, compute_resource.address);
            var type_str = my.pad(12, compute_resource.instanceOsType);
            var status_str = my.pad(10, compute_resource.status);
            console.log(ip_str, type_str, status_str, compute_resource.instanceId)
        })
    })
}

function listAwsInstances() {
    console.log();
    allAwsUserInstances.sort(sortInstances)
        .forEach(function(res){
            var user_str = my.pad(20,res.userName);
            var type_str = my.pad(18, res.instanceType);
            var node_str = my.pad(14, res.instanceId);
            var ip_str = my.pad(16, res.address);
            var cpu_str = my.pad(4, res.coreCount);
            var status_str = my.pad(15, res.status);
            var date_str = getLaunchTimeStr(res);

            var vpg_state_str = '';
            if (res.status !== 'terminated') {
                if (res.vpgId) {
                    var stackId = my.findElementInArray(allStacks, 'id', res.vpgId);
                    if (stackId)
                        vpg_state_str = 'Ok.';
                    else
                        vpg_state_str = '__INVALID VPG stack id '+res.vpgId;
                } else {
                    vpg_state_str = '___ORPHAN___';
                }
            }

            console.log(user_str, type_str, node_str, ip_str, cpu_str, status_str, date_str, vpg_state_str)
        })
}

function listComputeResources() {
    console.log();
    allComputeResources.sort(sortInstances)
        .forEach(function (res) {
            var res_stack = my.findElementInArray(allStacks, 'id', res.vpgId);
            var stack_awsInstance = my.findElementInArray(allAwsUserInstances, 'vpgId', res.vpgId);
            var res_awsInstance = my.findElementInArray(allAwsUserInstances, 'instanceId', res.instanceId);

            var user_str = my.pad(20, res.userName);
            var type_str = my.pad(18, res.instanceType);
            var node_str = my.pad(14, res.instanceId);
            var ip_str = my.pad(16, res.address);
            var cpu_str = my.pad(4, res.coreCount);
            var status_str = my.pad(15, res.status);
            var date_str = getLaunchTimeStr(res);

            var vpg_state_str = '';
            if (res_stack && stack_awsInstance && res_awsInstance)
                vpg_state_str = 'Ok.';
            else {
                if (!res_stack)
                    vpg_state_str += ' Stack with ID '+res.vpgId+' not found in GHAP stacks';
                if (!stack_awsInstance)
                    vpg_state_str += ' Stack with ID '+res.vpgId+' not found on AWS';
                if (!res_awsInstance)
                    vpg_state_str += ' instanceId not found on AWS';
            }

            console.log(user_str, type_str, node_str, ip_str, cpu_str, status_str, date_str, vpg_state_str)
        })
}

/**
 * @param {computeResource | awsUserInstance} res
 * @returns {string}
 */
function getLaunchTimeStr(res) {
    return res.launchTime
        .replace(/T/, ' ')     // replace T with a space
        .replace(/\..+/, '')  // delete the dot and everything after
        .concat(' UTC  ');
}

function getUserNameById(user_id) {
    /**
     * @type {ghapUser | null}
     */
    var ghap_user = my.findElementInArray(allGhapUsers, 'guid', user_id);
    var user_name = 'NOT FOUND';
    if (ghap_user) user_name = ghap_user.name;
    return user_name;
}

function getActivityNameById(activity_id) {
    /**
     * @type {ghapActivity | null}
     */
    var ghap_activity = my.findElementInArray(allGhapActivities, 'id', activity_id);
    var activity_name = 'NOT FOUND';
    if (ghap_activity) activity_name = ghap_activity.activityName;
    return activity_name;
}

/**
 * @param {computeResource | awsUserInstance} res1
 * @param {computeResource | awsUserInstance} res2
 * @returns {number}
 */
function sortInstances(res1, res2) {
    setResourceUserName(res1);
    setResourceUserName(res2);
    setResourceInstanceType(res1);
    setResourceInstanceType(res2);

    if (res1.userName === res2.userName) {
        return res1.instanceType.localeCompare(res2.instanceType);
    }
    return res1.userName.localeCompare(res2.userName);
}

/**
 * Add username field to the resource and set it value
 * @param {computeResource | awsUserInstance} res
 */
function setResourceUserName(res) {
    if (!res.userName) {
        res.userName = 'NOT FOUND';
        var ghap_user = my.findElementInArray(allGhapUsers, 'guid', res.userId);
        if (ghap_user) res.userName = ghap_user.name;
    }
}

/**
 * Add instanceType field to the resource and set it value
 * @param {computeResource | awsUserInstance} res
 */
function setResourceInstanceType(res) {
    if (!res.instanceType) {
        var vpg_stack = my.findElementInArray(allStacks, 'id', res.vpgId);
        if (vpg_stack)
            res.instanceType = getInstanceType(res, vpg_stack);
        else
            res.instanceType = res.instanceOsType + ' ???';
    }
}

/**
 * @param {computeResource  | awsUserInstance} res
 * @param {vpgStack} vpg_stack - the stack for the compute resource
 * @returns {string}
 */
function getInstanceType(res, vpg_stack) {
    var type = res.instanceOsType;
    if (type === 'Linux') {
        var activity_name = getActivityNameById(vpg_stack.activityId).toLowerCase();
        if (  (activity_name.indexOf('virtual private grid') > -1)
           || (activity_name.indexOf(' vpg') > -1) ) {
            if (res.status === 'stopped')
                type = 'Linux VPG';
            else if (res.address)
                type = 'Linux VPG head';
            else
                type = 'Linux VPG node';
        } else {
            type = 'Linux host'
        }
    }
    return type;
}


function listStackDetails4User(user_guid) {
    /**
     * @type {ghapUser | null}
     */
    var ghap_user = my.findElementInArray(allGhapUsers, 'guid', user_guid);
    if (ghap_user) {
        var umsUser = require('../ums/ums_user');
        var ums_user = umsUser.makeUserFromGhapUser(ghap_user)
    } else {
        console.log("User with id '%s' not found.", user_guid)
        return;
    }

    var user_stacks = [];
    return psRequests.multiVpgGetStatuses4User(oAuth.header, ums_user, user_stacks)
        .then(function(){
            console.log("\nUser %s has %d stacks:", ums_user.getName(), user_stacks.length);
            user_stacks.forEach(function(stack){
                console.log("   Stack with activity '%s' has %d compute resources:",
                    getActivityNameById(stack.activityId), stack.computeResources.length);
                stack.computeResources.forEach(function(res) {
                    var space_str = my.pad(20);
                    var type_str = my.pad(18, res.instanceOsType);
                    var node_str = my.pad(14, res.instanceId);
                    var ip_str = my.pad(16, res.address);
                    var cpu_str = my.pad(4, res.coreCount);
                    var status_str = my.pad(15, res.status);
                    var date_str = getLaunchTimeStr(res);
                    console.log(space_str, type_str, node_str, ip_str, cpu_str, status_str, date_str)
                })
            })
        });
}