'use strict';

describe('Controller: GroupManagementCreateGroupsCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var GroupManagementCreateGroupsCtrl,
    scope;

  var groups = [];

  var serverError = {data: {errors: [
    {
      field: 'name',
      errors: [{message: 'name field is empty'}]
    }
  ]}};

  var Group = {
    save: function(group, success, error){
      if(group.name === ''){
        error(serverError);
      } else {
        groups.push(group);
        success(group);
      }
    },
    query: function(success, error){
      if(groups === null){
        error(serverError);
      } else {
        success(groups);
      }
    },
    get: function(){

    }
  };

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    GroupManagementCreateGroupsCtrl = $controller('GroupManagementCreateGroupsCtrl', {
      $scope: scope,
      Group: Group
    });
  }));

  it('$scope.add should create new group', function () {
    scope.group = {};
    scope.errors = [];
    scope.groups = [];
    scope.group = {name: 'NewGroupName'};

    scope.add();

    expect(scope.groups.length).toBe(1);
    expect(scope.errors.length).toBe(0);
  });

  it('$scope.add should process errors', function () {
    scope.group = {};
    scope.errors = [];
    scope.groups = [];
    scope.group = {name: ''};

    scope.add();

    expect(scope.groups.length).toBe(0);
    expect(scope.errors.length).toBe(1);
  });

  it('$scope.reset should reset new group', function () {
    scope.group = {};
    scope.groups = [];
    scope.group = {name: 'NewGroupName'};

    scope.reset();

    expect(scope.group.name).toBe('');
  });

  it('$scope.loadGroups should load groups', function () {
    groups = [{dn: 'my dn', name: 'my name'}];
    scope.group = {};
    scope.groups = [];
    scope.group = {name: 'NewGroupName'};

    scope.loadGroups();

    expect(scope.groups.length).toBe(1);
  });

  it('$scope.loadGroups should process errors', function () {
    groups = null;
    scope.group = {};
    scope.groups = [];
    scope.group = {name: 'NewGroupName'};

    scope.loadGroups();

    expect(scope.groups.length).toBe(0);
  });

});
