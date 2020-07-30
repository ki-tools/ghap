'use strict';

describe('Controller: GroupManagementAssignUsersCtrl', function () {

  // load the controller's module
  beforeEach(module('bmgfApp', function ($provide) {
    mockServerCalls($provide);
  }));

  var GroupManagementCreateGroupsCtrl,
    scope;

  var groups = [];
  var users = [];

  var serverError = {data: {errors: [
    {
      field: 'name',
      errors: [{message: 'name field is empty'}]
    }
  ]}};


  var Group = {
    save: function(group, success){
      groups.push(group);
      success(group);
    },
    query: function(query, success, error){
      //var dn = query.dn;
      var action = query.action;
      if(query.dn === ''){
        error(serverError);
      } else if(action === 'users'){
        success(users);
      } else {
        success(groups);
      }
    },
    get: function(){

    }
  };

  var User = {
    list: function(success){
      success(users);
    }
  };

  var groupInstanceMethods = {
    $addMember: function(query, success, error){
      if(query.udn === ''){
        error();
      } else {
        users.push({dn: 'newAssignedUser', query: query});
        success();
      }
    },
    $deleteMember: function(query, success, error){
      if(query.udn === ''){
        error();
      } else {
        users.splice(0, users.length-1);
        success();
      }
    }
  };

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    GroupManagementCreateGroupsCtrl = $controller('GroupManagementAssignUsersCtrl', {
      $scope: scope,
      Group: Group,
      User: User
    });
  }));

  it('$scope.getUsers should users for group', function () {

    var user = {
      dn: 'userDn'
    };
    users = [user];
    groups = [{
      dn: 'groupDn'
    }];

    scope.groups = [];
    scope.users = [user];

    scope.groupFilter = {name: ''};
    scope.userFilter = {name: ''};
    scope.groups = [];
    scope.currentGroup = {};
    scope.groupUsers = [];


    scope.getUsers({dn: 'groupDn'});

    expect(scope.users.length).toBe(1);
    expect(scope.users[0].selected).toBe(true);
  });

  it('$scope.getUsers should process errors', function () {

    var user = {
      dn: 'userDn'
    };
    users = [user];
    groups = [{
      dn: 'groupDn'
    }];

    scope.groups = [];
    scope.users = [user];

    scope.groupFilter = {name: ''};
    scope.userFilter = {name: ''};
    scope.groups = [];
    scope.currentGroup = {};
    scope.groupUsers = [];


    scope.getUsers({dn: ''});

    expect(scope.users.length).toBe(1);
    expect(scope.error).toBe(serverError.data);
    expect(!!scope.users[0].selected).toBe(false);
  });

  it('$scope.saveUsers should assign user to group', function () {

    var user = {
      dn: 'userDn'
    };
    users = [user];
    groups = [{
      dn: 'groupDn'
    }];

    //scope.success = 'not empty';

    scope.groups = [];
    scope.users = [user];

    scope.groupFilter = {name: ''};
    scope.userFilter = {name: ''};
    scope.groups = [];

    var group = {dn: 'currentGroup'};
    angular.extend(group, groupInstanceMethods);

    scope.currentGroup = group;
    scope.groupUsers = [];


    scope.saveUsers();

    expect(scope.success).toBe('');
    expect(users.length).toBe(1);

    user.selected = true;
    scope.saveUsers();

    expect(scope.success).toBe('User(s) successfully (un)assigned to group');
    expect(users.length).toBe(2);

    user.selected = false;
    scope.saveUsers();

    expect(scope.success).toBe('User(s) successfully (un)assigned to group');
    expect(users.length).toBe(1);
  });

  it('$scope.saveUsers should process errors', function () {

    var user = {
      dn: ''
    };
    users = [user];
    groups = [{
      dn: 'groupDn'
    }];

    //scope.success = 'not empty';

    scope.groups = [];
    scope.users = [user];

    scope.groupFilter = {name: ''};
    scope.userFilter = {name: ''};
    scope.groups = [];

    var group = {dn: 'currentGroup'};
    angular.extend(group, groupInstanceMethods);

    scope.currentGroup = group;
    scope.groupUsers = [];


    scope.saveUsers();

    expect(scope.success).toBe('');
    expect(users.length).toBe(1);

    user.selected = true;
    scope.saveUsers();

    expect(scope.success).toBe('');
    expect(users.length).toBe(1);

    user.selected = false;
    scope.saveUsers();

    expect(scope.success).toBe('');
    expect(users.length).toBe(1);
  });

});
