'use strict';

describe('Controller: ModelingActivitiesCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var ModelingActivitiesCtrl,
    scope;

  var activities = [];

  var Activity = function(activity){
    this.id = activity.id;
    this.name = activity.name;
  };

  Activity.prototype.$create = function(cb){
    //activities.push(new Activity({id: activities.length}));
    this.id = activities.length;
    activities.push(this);
    cb();
  };
  Activity.prototype.$save = function(cb){
    for(var i=0; i < activities.length;i++){
      var a = activities[i];
      if(this.id === a.id){
        a.name = this.name;
        break;
      }
    }
    cb();
  };
  Activity.prototype.$delete = function(cb){
    for(var i=0; i < activities.length;i++){
      var a = activities[i];
      if(this.id === a.id){
        activities.splice(i,1);
        break;
      }
    }
    cb();
  };

  Activity.query = function(){
    return activities;
  };

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    ModelingActivitiesCtrl = $controller('ModelingActivitiesCtrl', {
      $scope: scope,
      Activity: Activity
    });
  }));

  it('$scope.select should assign activity to a scope', function () {
    scope.activity = {};
    var activity = {id: 1};

    scope.select(activity);

    expect(scope.activity.id).toBe(1);
  });

  it('$scope.reset should reset activity in a scope', function () {
    scope.activity = {id: 1};

    scope.reset();

    expect(scope.activity.id === 1).toBe(false);
  });

  it('$scope.save should create activity', function () {
    var newActivityName = 'new activity';
    scope.activity = new Activity({id:null, name: newActivityName});
    activities = [];

    scope.save();

    expect(scope.activities.length).toBe(1);
    expect(scope.activities[0].id).not.toBeNull();
    expect(scope.activities[0].name).toBe(newActivityName);
  });

  it('$scope.save should update activity', function () {
    var activityNewName = 'myname';
    var activityId = 155;
    scope.activity = new Activity({id: activityId, name: activityNewName});
    activities = [new Activity({id: activityId, name:'old name'})];

    scope.save();

    expect(scope.activities.length).toBe(1);
    expect(scope.activities[0].id).toBe(activityId);
    expect(scope.activities[0].name).toBe(activityNewName);
  });

  it('$scope.remove should delete activity and update scope', function () {
    activities= [{id:12},{id:5}];

    scope.remove(new Activity({id:12}));

    expect(scope.activities.length).toBe(1);
    expect(scope.activities[0].id).toBe(5);
  });
});
