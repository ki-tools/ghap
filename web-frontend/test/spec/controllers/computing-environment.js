'use strict';

/*

$scope.internalActivities - all activities available for user
$scope.activities - activities that can be launched
$scope.modelActivities - activities selected in dialog

 */

describe('Controller: ComputingEnvironmentCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var ComputingEnvironmentCtrl, scope, locationPath,
      currentUser, currentUserActivities, currentUserStacks, currentUserResources;

  var User = {
    getCurrentUser: jasmine.createSpy('getCurrentUser').and.callFake(
      function(callback) {callback(currentUser);}
    ),
    getActivities: jasmine.createSpy('getActivities').and.callFake(
      function(callback) {callback(currentUserActivities);}
    )
  };

  var Stack = {
    terminate: jasmine.createSpy('Stack.terminate'),
    create:    jasmine.createSpy('Stack.create'),
    query:     jasmine.createSpy('Stack.query')
      .and.callFake(function(user, successFn, errFn){
        //jshint unused:false
        if (user.guid === currentUser.guid) {
          successFn(currentUserStacks);
        }
      }),
    computeResources: jasmine.createSpy('Stack.computeResources')
      .and.callFake(function(user, successFn, errFn){
        //jshint unused:false
        if (user.guid === currentUser.guid) {
          successFn(currentUserResources);
        }
      }),
    rdpFileUrl: jasmine.createSpy('Stack.rdpFileUrl')
      .and.callFake(function(resource){
          return 'rdp://'+resource.instanceId;
      })
  };

  var location = {
    path: function() {return locationPath;}
  };

  var interval = jasmine.createSpy('interval');

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    currentUser = {quid:'812'};
    currentUserActivities = [
      {id:'win-activity-0', os:'Windows'},
      {id:'win-activity-1', os:'Windows'},
      {id:'linux-activity-0', os:'Linux'},
      {id:'linux-activity-1', os:'Linux'}
    ];
    currentUserStacks =[];
    currentUserResources =[];
    locationPath = '/computing-environment';
    scope = $rootScope.$new();
    ComputingEnvironmentCtrl = $controller('ComputingEnvironmentCtrl', {
      $scope:   scope,
      Stack:    Stack,
      User:     User,
      $location: location,
      $timeout: function(callback){ callback(); },
      $interval: interval
    });
  }));

  describe('$scope.removeById', function () {
    
    it('should remove item from arr by id', function () {
      var arr = [ {id: 1}, {id: 2}, {id: 3} ];
      arr = scope.removeById(arr, 2);
      expect(arr.length).toBe(2);
    });

    it('should not modify array of no item found', function () {
      var arr = [ {id: 1}, {id: 2}, {id: 3} ];
      arr = scope.removeById(arr, 20);
      expect(arr.length).toBe(3);
    });

  });

  describe('$scope.push2Pending', function () {
    
    it('should push obj to pendingStacks if it is not there', function () {
      scope.pendingStacks = [
        {activityId: 1}, {activityId: 2}, {activityId: 3}
      ];
      var activityId = 20;
      scope.push2Pending({id: activityId, os: 'windows'});
      expect(scope.pendingStacks.length).toBe(4);
      expect(scope.pendingStacks[3].activityId).toBe(activityId);
      expect(scope.pendingStacks[3].resources.main.instanceOsType).toBe('Windows');
      expect(scope.pendingStacks[3].resources.main.status).toBe('initializing');
    });

    it('should not push obj to pendingStacks if it is already there', function () {
      scope.pendingStacks = [
        {activityId: 1}, {activityId: 2}, {activityId: 3}
      ];
      scope.push2Pending({id: 2});
      expect(scope.pendingStacks.length).toBe(3);
    });

  });

  describe('$scope.getActivityById', function () {

    it('should return activity by it\'s id', function () {
      scope.internalActivities = [
        {id: 1}, {id: 'a2', name: 'activity'}, {id: 3}
      ];
      var a = scope.getActivityById('a2');
      expect(a).toBe(scope.internalActivities[1]);
    });
    
    it('should return null if activity not found', function () {
      scope.internalActivities = [
        {id: 1}, {id: 'a2', name: 'activity'}, {id: 3}
      ];
      var a = scope.getActivityById('a1');
      expect(a).toBe(null);
    });

  });

  describe('on controller load', function(){
    // onLoad
    // User.getCurrentUser
    // -> $scope.initActivities
    // -> $scope.computeResources
    // -> $scope.getStacks
    // -> Stack.query
    it('should get current user and init $scope.internalActivities', function(){
      expect(scope.user).toBe(currentUser);
      expect(scope.internalActivities).toEqual(currentUserActivities);
    });

    it('should enable all activities if user has no stacks ', function(){
      expect(scope.activities).toEqual(scope.internalActivities);
    });

  });

  describe('$computeResources', function(){

    beforeEach(function(){
      scope.getStacks = jasmine.createSpy('getStacks');
    });

    it('should call getStacks if current page is /computing-environment', function(){
      interval.calls.reset();
      scope.computeResources();
      expect(scope.getStacks).toHaveBeenCalled();
      expect(interval).toHaveBeenCalled();
    });

    it('should do nothing if current path is not /computing-environment', function(){
      locationPath = '';
      interval.calls.reset();
      scope.computeResources();
      expect(scope.getStacks).not.toHaveBeenCalled();
      expect(interval).not.toHaveBeenCalled();
    });

  });

  describe('$scope.getStacks', function(){

    var winStack, winResource, linuxStack, linuxResources;
    beforeEach(function(){

      winStack = {
        activityId: 'windows-activity-0',
        stackId:'stack-id0'
      };
      winResource = {
        stackId: 'stack-id0',
        instanceOsType: 'Windows',
        instanceId: 'win-instance-id',
        address: '0.0.0.0',
        status: 'running'
      };

      linuxStack = {
        activityId: 'linux-activity-0',
        stackId:'stack-id1'
      };
      linuxResources = [
        { stackId: 'stack-id1',
          instanceOsType: 'Linux',
          instanceId: 'linux-instance-id1',
          isTopLevelNode: false
        },
        { stackId: 'stack-id1',
          instanceOsType: 'Linux',
          instanceId: 'linux-instance-id2',
          address: '0.0.0.0',
          isTopLevelNode: true
        }
      ];
    });

    it('should remove pending activities from list for launching', function(){
      scope.push2Pending(currentUserActivities[0]);
      scope.push2Pending(currentUserActivities[2]);

      expect(scope.activities.length).toBe(currentUserActivities.length);
      scope.getStacks();
      expect(scope.activities.length).toBe(0);
    });

    it('should push to stacks user\'s resources and set main for linux VPG', function(){
      currentUserStacks.push(linuxStack);
      currentUserResources = linuxResources;

      scope.getStacks();
      expect(scope.stacks.length).toBe(1);
      expect(scope.stacks[0].activityId).toBe(linuxStack.activityId);
      expect(scope.stacks[0].resources.main).toEqual(linuxResources[1]);
    });

    it('should set rdp url for windows instance', function(){
      currentUserStacks.push(winStack);
      currentUserResources.push(winResource);

      scope.getStacks();
      expect(Object.keys(scope.rdps).length).toBe(1);
      expect(scope.rdps[winResource.instanceId]).toBe('rdp://'+winResource.instanceId);
    });

    it('should remove provisioned activities from available list', function(){
      currentUserStacks.push(linuxStack);
      currentUserResources = linuxResources;
      currentUserStacks.push(winStack);
      currentUserResources.push(winResource);

      expect(scope.activities.length).toBe(currentUserActivities.length);
      scope.getStacks();
      expect(scope.activities.length).toBe(0);
    });

  });

  describe('$scope.create', function () {

    it('should hide dialog and clear activities selection in dialog', function () {
      scope.showVirtEnvDialog = true;
      scope.modelActivities[currentUserActivities[1].id] = true;
      scope.modelActivities[currentUserActivities[2].id] = false;

      scope.create();

      expect(scope.showVirtEnvDialog).toBe(false);
      expect(scope.modelActivities).toEqual({});
    });

    it('should push launched ativities to scope.pendingStacks', function(){
      scope.modelActivities[currentUserActivities[0].id] = true;
      scope.modelActivities[currentUserActivities[1].id] = false;
      scope.modelActivities[currentUserActivities[2].id] = true;

      scope.create();

      expect(scope.pendingStacks.length).toBe(2);

    });

  });

  describe('$scope.toggleStack', function () {

    it('should call pause if resource is running', function () {
      scope.pause = jasmine.createSpy('pause');
      var resource = {status: 'running'};
      var id = 1;

      scope.toggleStack(id, resource);

      expect(scope.pause).toHaveBeenCalledWith(id);
      expect(resource.status).toBe('stopping');
    });

    it('should call resume if resource is stopped', function () {
      scope.resume = jasmine.createSpy('resume');
      var resource = {status: 'stopped'};
      var id = 1;

      scope.toggleStack(id, resource);

      expect(scope.resume).toHaveBeenCalledWith(id);
      expect(resource.status).toBe('pending');
    });

    it('should do nothing if resource is in progress', function () {
      scope.resume = jasmine.createSpy('resume');
      scope.pause = jasmine.createSpy('pause');
      var resource = {status: 'pending'};
      var id = 1;

      scope.toggleStack(id, resource);

      expect(scope.resume).not.toHaveBeenCalledWith(id);
      expect(scope.pause).not.toHaveBeenCalledWith(id);
    });

  });

  describe('$scope.isOn', function () {

    it('should return true if it is in one of states \'pending\', \'initializing\', \'running\'', function () {
      ['pending', 'initializing', 'running'].forEach(function(val) {
        var resource = {status: val};
        expect(scope.isOn(resource)).toBe(true);
      });
    });

    it('should return false if it is in one of states \'stopping\', \'stopped\'', function () {
      ['stopping', 'stopped'].forEach(function(val) {
        var resource = {status: val};
        expect(scope.isOn(resource)).toBe(false);
      });
    });

    it('should return false if it is in unknown status', function () {
      ['unknown0', 'unknown1'].forEach(function(val) {
        var resource = {status: val};
        expect(scope.isOn(resource)).toBe(false);
      });
    });

  });

  describe('$scope.isDisabled', function () {

    it('should return false if it is in pending state', function () {
      ['pending', 'initializing', 'stopping'].forEach(function(val) {
        var resource = {status: val};
        expect(scope.isDisabled(resource)).toBe(true);
      });
    });

    it('should return true if it is in final state', function () {
      ['running', 'stopped'].forEach(function(val) {
        var resource = {status: val};
        expect(scope.isDisabled(resource)).toBe(false);
      });
    });

  });

  describe('$scope.initActivities', function () {

    it('should initialize os field of an activity if it is not set and call for resources', function () {
      var activities = [{os: 'win', id: 1}, {id: 2}];
      scope.computeResources = jasmine.createSpy('computeResources');

      scope.initActivities(activities);

      expect(scope.computeResources).toHaveBeenCalled();
      expect(scope.internalActivities).toEqual(activities);
    });

  });

  it('$scope.terminate should call Stack.terminate with user guid', function () {
    var user = {guid: 1};
    var activityId = 2;
    scope.user = user;
    scope.getStacks = jasmine.createSpy('getStacks');

    scope.terminate(activityId);

    expect(Stack.terminate).toHaveBeenCalledWith(user.guid, activityId);
    expect(scope.getStacks).toHaveBeenCalled();
  });

});
