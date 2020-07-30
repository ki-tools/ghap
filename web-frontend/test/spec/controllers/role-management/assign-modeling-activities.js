'use strict';

describe('Controller: RoleManagementAssignModelingActivitiesCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var RoleManagementAssignModelingActivitiesCtrl, Role, Activity, ActivityRoleAssociation,
    scope;

  var activities = [{id: '1'}];
  var activityRoleAssociation = [];

  var roles = [
    {dn: 'testRoleDn', name: 'testRole'}
  ];

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();


    ActivityRoleAssociation = {
      query: jasmine.createSpy('ActivityRoleAssociation.query').and.callFake(function(params, success, error) {
        if(params.guid){
          success(activityRoleAssociation);
        } else {
          error({data: 'some error'});
        }

      }),
      delete: jasmine.createSpy('ActivityRoleAssociation.delete').and.callFake(function(params, success) {
        success();
      }),
      save: jasmine.createSpy('ActivityRoleAssociation.save').and.callFake(function(params, activities, success) {
        success();
      })
    };

    Activity = {
      query: jasmine.createSpy('Activity.query').and.callFake(function(success) {
        success(activities);
      })
    };

    Role = {
      query: jasmine.createSpy('query').and.callFake(function(success) {
        success(roles);
      }),
      get: jasmine.createSpy('get').and.callFake(function(params) {

      }),
      add: jasmine.createSpy('add').and.callFake(function(params, success, error) {
        if(params.action){
          success({data: 'some data'});
        } else {
          error({data: 'some error'});
        }
      }),
      remove: jasmine.createSpy('remove').and.callFake(function(params, success, error) {
        if(params.action){
          success({data: 'some data'});
        } else {
          error({data: 'some error'});
        }
      }),
      save: jasmine.createSpy('save').and.callFake(function(params, success, error) {
        if(params.description){
          success({data: 'some data'});
        } else {
          error(
            {
              data: {
                errors: [
                  {
                    field: 'name', 
                    errors: [{message: 'some error'}]
                  }
                ]
              }
            }
          );

        }
      })
    };


    RoleManagementAssignModelingActivitiesCtrl = $controller('RoleManagementAssignModelingActivitiesCtrl', {
      $scope: scope,
      Role: Role,
      Activity: Activity,
      ActivityRoleAssociation: ActivityRoleAssociation
    });
  }));

  it('should get role list during controller initialization', function () {
    expect(scope.errors.length).toBe(0);
    expect(scope.roles.length).toBe(roles.length);
    expect(Role.query).toHaveBeenCalledWith(jasmine.any(Function), jasmine.any(Function));
    expect(Object.keys(scope.roleActivities).length).toBe(activities.length);
  });

  it('should get Activity list during controller initialization', function () {
    expect(scope.errors.length).toBe(0);
    expect(scope.activities.length).toBe(roles.length);
    expect(Activity.query).toHaveBeenCalledWith(jasmine.any(Function), jasmine.any(Function));
  });

  it('$scope.getActivities should process errors', function () {
    scope.errors = [];
    var role = {guid: ''};
    scope.getActivities({guid: role.guid});
    expect(scope.errors.length).toBe(1);
  });


  it('$scope.getActivities should get ActivityRoleAssociation for specified role', function () {
    var role = {guid: '1'};
    scope.getActivities({guid: role.guid});
    expect(scope.errors.length).toBe(0);
    expect(ActivityRoleAssociation.query).toHaveBeenCalledWith({guid: role.guid}, jasmine.any(Function), jasmine.any(Function));
  });

  it('$scope.save should delete all previous ActivityRoleAssociation and create new ones for selected role', function () {
    scope.selectedRole = {guid: '1'};
    var activities = [];
    scope.save();
    expect(scope.errors.length).toBe(0);
    expect(ActivityRoleAssociation.delete).toHaveBeenCalledWith({guid: scope.selectedRole.guid}, jasmine.any(Function));
    expect(ActivityRoleAssociation.save).toHaveBeenCalledWith({guid: scope.selectedRole.guid}, activities, jasmine.any(Function));
  });



});
