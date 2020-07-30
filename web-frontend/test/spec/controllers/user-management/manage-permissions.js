'use strict';

describe('Controller: UserManagementManagePermissionsCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var UserManagementManagePermissionsCtrl,
      scope,
      User,
      Role;


  var users = [
    {
      name: 'user1',
      dn: 'dn1',
      guid: '1'
    },
    {
      name: 'user2',
      dn: 'dn2',
      guid: '2'
    }
  ];

  var currentUser = {
    name: 'CurrentUser',
    dn: 'currentuserDn',
    guid: '100'
  };

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();

    User = {
      getCurrentUser: jasmine.createSpy('getCurrentUser').and.callFake(function(callback) {
        callback(currentUser);
      }),
      list: jasmine.createSpy('list').and.callFake(function(success) {
        success(users);
      }),
      query: jasmine.createSpy('query').and.callFake(function(params, success, error) {
        if(params.dn){
          success(users);
        } else {
          error({data: 'some error'});
        }
      }),
      get: jasmine.createSpy('get').and.callFake(function(userData, success, error) { 
        if(userData.dn){
          success(userData);
        } else {
          error({data: 'some error'});
        }
      })
    };

    Role = {
      query: jasmine.createSpy('query').and.callFake(function(success) {
        success(users);
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
      })
    };

    UserManagementManagePermissionsCtrl = $controller('UserManagementManagePermissionsCtrl', {
      $scope: scope,
      User: User,
      Role: Role
    });

  }));

  it('should get user list during controller initialization', inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    scope.users = [];
    UserManagementManagePermissionsCtrl = $controller('UserManagementEditAccountCtrl', {
      $scope: scope,
      User: User,
      Role: Role
    });
    expect(scope.errors.length).toBe(0);
    expect(scope.users.length).toBe(users.length);
  }));

  it('should show error for wrong User.list response', inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    User.list = jasmine.createSpy('list').and.callFake(function(success, error) {
      error({data: 'some error'});
    })
    UserManagementManagePermissionsCtrl = $controller('UserManagementManagePermissionsCtrl', {
      $scope: scope,
      User: User,
      Role: Role
    });
    expect(scope.errors.length).toBe(1);
  }));

  it('should show error for wrong Role.query response', inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    Role.query = jasmine.createSpy('query').and.callFake(function(success, error) {
      error({data: 'some error'});
    })
    UserManagementManagePermissionsCtrl = $controller('UserManagementManagePermissionsCtrl', {
      $scope: scope,
      User: User,
      Role: Role
    });
    expect(scope.errors.length).toBe(1);
  }));

  it('should show error for wrong scope.getRoles response', inject(function ($controller, $rootScope) {
    expect(scope.errors.length).toBe(0);
    scope.getRoles(null);
    expect(scope.errors.length).toBe(1);
  }));

  it('should add role', inject(function ($controller, $rootScope) {
    scope.roles = [{
      dn: 'roleDn',
      selected: true
    }];
    scope.currentUserDn = 'currentUserDn';
    scope.userRoles = [];
    scope.saveRoles();
    expect(Role.add).toHaveBeenCalledWith({action: 'roleDn', udn: scope.currentUserDn}, jasmine.any(Function), jasmine.any(Function));
  }));

  it('should remove role', inject(function ($controller, $rootScope) {
    scope.roles = [{
      dn: 'roleDn',
      selected: false
    }];
    scope.currentUserDn = 'currentUserDn';
    scope.userRoles = [
    {
      dn: 'roleDn'
    }];
    scope.saveRoles();
    expect(Role.remove).toHaveBeenCalledWith({action: 'roleDn', udn: scope.currentUserDn}, jasmine.any(Function), jasmine.any(Function));
  }));

});
