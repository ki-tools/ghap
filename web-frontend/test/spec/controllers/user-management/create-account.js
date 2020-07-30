'use strict';

describe('Controller: UserManagementCreateAccountCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var UserManagementCreateAccountCtrl, User, PersonalStorage,
    scope;

  var users = [];

  var serverError = {data: {errors: [
    {
      field: 'name',
      errors: [{message: 'name field is empty'}]
    }
  ]}};


  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();

    User = {
      save: jasmine.createSpy('save').and.callFake(function(user, success, error){
              if(user.name === ''){
                error(serverError);
              } else {
                users.push(user);
                success(user);
              }
            })
    };

    PersonalStorage = {
      create: jasmine.createSpy('create').and.callFake(function(params, success, error){
              if( !params.guid ){
                error(serverError);
              } else {
                success({});
              }
            })
    };

    UserManagementCreateAccountCtrl = $controller('UserManagementCreateAccountCtrl', {
      $scope: scope,
      User: User,
      PersonalStorage: PersonalStorage
    });
  }));

  it('$scope.submit should create new user', function () {
    scope.user = {};
    scope.errors = [];
    users = [];
    scope.user = {name: 'NewUserName'};

    scope.submit();

    expect(users.length).toBe(1);
    expect(scope.errors.length).toBe(0);
    expect(!!scope.success).toBe(true);
  });

  it('$scope.submit should process errors', function () {
    scope.user = {};
    scope.errors = [];
    scope.users = [];
    scope.user = {name: ''};

    scope.submit();

    expect(scope.users.length).toBe(0);
    expect(scope.errors.length).toBe(1);
  });

  it('$scope.reset should reset a new user', function () {
    scope.user = {};
    scope.users = [];
    scope.user = {name: 'NewUserName'};

    scope.reset();

    expect(scope.user.name).toBe('');
  });

  it('$scope.submit should create a new PersonalStorage', function () {
    scope.user = {};
    scope.errors = [];
    users = [];
    scope.user = {name: 'NewUserName', storage: true, guid: 1};

    scope.submit();

    expect(PersonalStorage.create).toHaveBeenCalled();
    expect(scope.errors.length).toBe(0);
  });

  it('$scope.submit should show error for PersonalStorage "create"', function () {
    scope.user = {};
    scope.errors = [];
    users = [];
    scope.user = {name: 'NewUserName', storage: true, guid: null};

    scope.submit();

    expect(PersonalStorage.create).toHaveBeenCalled();
    expect(scope.errors.length).toBe(1);
  });

});
