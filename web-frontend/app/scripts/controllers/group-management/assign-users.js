'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:GroupManagementManagePermissionsCtrl
 * @description
 * # GroupManagementManagePermissionsCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('GroupManagementAssignUsersCtrl', function ($scope, Group, User, $timeout, Settings) {

    // $scope.USERS_URL = Settings.API_ROOT + '/group/all/default';

    $scope.groupFilter = {name: ''};

    $scope.userFilter = {name: ''};

    $scope.groups = [];

    $scope.currentGroup = {};

    $scope.groupUsers = [];

    $scope.users = [];

    Group.query(function(groups) {
      $scope.groups = groups;
      var group = groups[0] || {dn: $routeParams.dn};
      $scope.getUsers(group);
    }, function(httpResponse) {
      $scope.error = httpResponse.data;
    });

    User.list(function(users) {
      $scope.users = users;
    }, function(httpResponse) {
      $scope.error = httpResponse.data;
    });

    $scope.getUsers = function(group) {
      $scope.currentGroup = group;
      Group.query({dn: group.dn, action: 'users'}, function(groupUsers) {
        $scope.groupUsers = groupUsers;
        for (var i = 0; i < $scope.users.length; i++) {
          var idx = groupUsers.map(function(x) { return x.dn; }).indexOf($scope.users[i].dn);
          $scope.users[i].selected = (idx != -1);
        }
      }, function(httpResponse) {
        $scope.error = httpResponse.data;
      });
    };

    $scope.saveUsers = function() {
      $scope.success = '';
      $scope.errors = [];
      var group = angular.copy($scope.currentGroup);
      for (var i = 0; i < $scope.users.length; i++) {
        var user = $scope.users[i];
        var idx = $scope.groupUsers.map(function(x) { return x.dn; }).indexOf(user.dn);
        if (user.selected && idx === -1) {
          group.$addMember({udn: user.dn}, function(data){
            $scope.success = 'User(s) successfully (un)assigned to group';
            $scope.getUsers(group);
          }, function(httpResponse){
            //console.log(httpResponse);
          });
        }

        if (!user.selected && idx != -1) {
          group.$deleteMember({udn: user.dn}, function(data){
            $scope.success = 'User(s) successfully (un)assigned to group';
            $scope.getUsers(group);
          }, function(httpResponse){
            //console.log(httpResponse);
          });
        }
      }
    };

  });
