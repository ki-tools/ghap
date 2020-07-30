'use strict';

describe('Controller: MyAccountCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var MyAccountCtrl,
    scope;


  var Activity = function(activity){
    this.id = activity.id;
    this.name = activity.name;

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

  Activity.query = function(){
    return activities;
  };

  var User = function(user){
    this.dn = user.dn;
    this.password = user.password;
  };

  var activities = [new Activity({id: 'someID', name: 'InitialActivity'})];
  var currentUser = new User({dn: 'currentUser'});

  User.getCurrentUser = function(callback){
    return callback(angular.copy(currentUser));
  };

  User.save = function(user, success, error){
    if(user.dn === currentUser.dn){
      currentUser = user;
      success(currentUser);
    } else {
      var newUser = new User({dn: 'currentUser'});
      var errorResponse = {
        data: {
          user: newUser,
          errors: [{
            field: 'password',
            errors: [{
              code: 'invalidCredentials'
            }]
          }]
        }
      };
      error(errorResponse);
    }
  };

  User.setCurrentUser = function(user){
    currentUser = angular.copy(user);
  };

  User.isResetPassword = function(){
    return false;
  };

  User.getActivities = function(callback){
    callback(activities);
  };

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    MyAccountCtrl = $controller('MyAccountCtrl', {
      $scope: scope,
      User: User
    });
  }));


  it('$scope.getCurrentUser should return current user', function () {
    expect(scope.user.dn).toBe(currentUser.dn);
  });

  it('$scope.update should update current user', function () {
    scope.user.name = 'mmm';
    scope.user.password = null;
    scope.user.passwordConfirmation = null;
    scope.update();
    expect(currentUser.name).toBe('mmm');
  });

  it('$scope.update should show password confirmation error', function () {
    scope.user.password = 'mmm';
    scope.user.passwordConfirmation = 'eee';
    scope.update();
    expect(scope.errors[0]).toBe('Invalid password confirmation.');
  });

  it('$scope.update should show password error', function () {
    scope.user.dn = 'invalid';
    scope.user.password = null;
    scope.user.passwordConfirmation = null;
    scope.update();
    expect(scope.errors.length).toBe(1);

  });


});
