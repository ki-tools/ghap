'use strict';

describe('Controller: InstanceManagementCtrl', function () {


  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var InstanceManagementCtrl, User, Stack, modal,
    scope;

  var activities = [];

  var Activity = function(activity){
    this.id = activity.id;
    this.name = activity.name;
    this.os = 'Windows';

    this.$save = function(){
      for(var i=0; i < activities.length;i++){
        var a = activities[i];
        if(this.id === a.id){
          a.name = this.name;
          break;
        }
      }
    };
    this.$create = function(){
      activities.push(new Activity({id: activities.length}));
    };
  };

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {

    activities = [];

    modal = {
      open: jasmine.createSpy('modal.open').and.callFake(function(params) {
        return {
          result: {
            then: jasmine.createSpy('modal.result.then').and.callFake(function(callback){
              callback();
            })
          }
        }
      })
    }

    User = function(name){
      this.name = name;
    };

    Stack = function(stack){
      this.stackId = stack.stackId;
    };
    Stack.computeResources = jasmine.createSpy('computeResources').and.callFake(function(params, callback, error) {
      callback([
        {
          stackId: 1,
          instanceOsType: 'Linux'
        }
      ])
    });
    Stack.terminate = jasmine.createSpy('Stack.terminate').and.callFake(function(params, activities) {});
    Stack.create = jasmine.createSpy('Stack.create').and.callFake(function(params, activities) {});
    Stack.pause = jasmine.createSpy('Stack.pause').and.callFake(function(params, activities) {});
    Stack.resume = jasmine.createSpy('Stack.resume').and.callFake(function(params, activities) {});


    Stack.query = jasmine.createSpy('query').and.callFake(function(params, callback) {
      callback([
        new Stack({stackId: 1})
      ])
    });

    var currentUser = new User('testUser');

    User.getActivities = jasmine.createSpy('getActivities').and.callFake(function(callback){
        callback(activities);
    });

    User.getCurrentUser = function(callback){
        callback(currentUser);
    };

    User.reportUsers = jasmine.createSpy('reportUsers').and.callFake(function(userIds, callback, error) {
      callback([]);
    });

    scope = $rootScope.$new();
    InstanceManagementCtrl = $controller('InstanceManagementCtrl', {
      $scope: scope,
      User: User,
      Stack: Stack,
      $modal: modal,
      $location: {
        path: function(){return '/admin-tools/instance-management'}
      },
      $interval: jasmine.createSpy('$interval'),
      $timeout: jasmine.createSpy('$timeout')
    });
  }));

  it('should call getStacks on load', function () {
    expect(Stack.query).toHaveBeenCalled();
    expect(scope.stacks.length).toBe(1);
  });

  it('should call User.getActivities on load', function () {
    expect(User.getActivities).toHaveBeenCalledWith( jasmine.any(Function) );
  });


  it('$scope.removeById should remove object from array by id', function () {
    var arr = [
      {id: 1},
      {id: 2}
    ];
    var result = scope.removeById(arr, 1);
    expect(result.length).toBe(1);
    expect(result[0].id).toBe(2);
  });

  it('$scope.push2Pending should push activity to pending state', function () {
    var activity = new Activity({id: 1, name: 'test'});
    scope.pendingStacks = [];

    scope.push2Pending(activity);
    expect(scope.pendingStacks.length).toBe(1);
  });

  it('$scope.getActivityById should get activity by id', function () {
    var activity = new Activity({id: 1, name: 'test'});
    scope.internalActivities = [activity];

    var result = scope.getActivityById(1);
    expect(result).toBe(activity);
  });

  it('$scope.terminate should terminate  activity', function () {
    //spyOn(scope, 'push2Pending');
    scope.terminate({
      activityId: 1,
      resources: {
        main: {status: 'stopped'}
      },
      user: {
        userid: 1
      }
    });
    expect(modal.open).toHaveBeenCalled();
    expect(Stack.terminate).toHaveBeenCalled();
    //expect(Stack.create).toHaveBeenCalled();
  });

  it('$scope.pause should pause  activity', function () {
    //spyOn(scope, 'push2Pending');
    scope.pause(1, 1);
    expect(Stack.pause).toHaveBeenCalled();
    //expect(Stack.create).toHaveBeenCalled();
  });

  it('$scope.resume should resume  activity', function () {
    //spyOn(scope, 'push2Pending');
    scope.resume(1, 1);
    expect(Stack.resume).toHaveBeenCalled();
    //expect(Stack.create).toHaveBeenCalled();
  });

  it('$scope.toggleStack should pause activity if running', function () {
    //spyOn(scope, 'push2Pending');
    scope.toggleStack({
      activityId: 1,
      resources: {
        main: {status: 'running'}
      },
      user: {
        userid: 1
      }
    });
    expect(Stack.pause).toHaveBeenCalled();
    //expect(Stack.create).toHaveBeenCalled();
  });

  it('$scope.toggleStack should resume activity if not running', function () {
    //spyOn(scope, 'push2Pending');
    scope.toggleStack({
      activityId: 1,
      resources: {
        main: {status: 'stopped'}
      },
      user: {
        userid: 1
      }
    });
    expect(Stack.resume).toHaveBeenCalled();
    //expect(Stack.create).toHaveBeenCalled();
  });

});
