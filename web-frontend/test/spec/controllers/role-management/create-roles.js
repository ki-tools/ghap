'use strict';

describe('Controller: RoleManagementCreateRolesCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var RoleManagementCreateRolesCtrl,
    Role,
    scope;

  var roles = [
    {dn: 'testRoleDn', name: 'testRole'}
  ];

  var serverError = {data: {errors: [
    {
      field: 'name',
      errors: [{message: 'name field is empty'}]
    }
  ]}};


  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();

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


    RoleManagementCreateRolesCtrl = $controller('RoleManagementCreateRolesCtrl', {
      $scope: scope,
      Role: Role
    });
  }));

  it('should get role list during controller initialization', function () {
    expect(scope.errors.length).toBe(0);
    expect(scope.roles.length).toBe(roles.length);
    expect(Role.query).toHaveBeenCalledWith(jasmine.any(Function), jasmine.any(Function));
  });

  it('scope.loadRoles should get role list', function () {
    Role.query = jasmine.createSpy('query').and.callFake(function(success) {
      success(roles);
    });

    scope.loadRoles();

    expect(scope.errors.length).toBe(0);
    expect(scope.roles.length).toBe(roles.length);
    expect(Role.query).toHaveBeenCalledWith(jasmine.any(Function), jasmine.any(Function));
  });

  it('scope.reset should reset role name to empty', function () {
    scope.role.name = 'testName'
    scope.reset();

    expect(scope.role.name).toBe('');
  });

  it('scope.add should invoke Role.save', function () {
    scope.role = {name: 'testName'}
    scope.reset = jasmine.createSpy('reset');
    Role.query = jasmine.createSpy('query');
    scope.add();

    expect(Role.save).toHaveBeenCalledWith(scope.role, jasmine.any(Function), jasmine.any(Function));
    expect(scope.reset).toHaveBeenCalled();
    expect(Role.query).toHaveBeenCalledWith(jasmine.any(Function), jasmine.any(Function));
  });

  it('scope.add should process errors', function () {
    scope.role = {name: ''}
    scope.reset = jasmine.createSpy('reset');
    Role.query = jasmine.createSpy('query');
    scope.add();

    expect(Role.save).toHaveBeenCalledWith(scope.role, jasmine.any(Function), jasmine.any(Function));
    expect(scope.errors.length).toBe(1);
  });


});
