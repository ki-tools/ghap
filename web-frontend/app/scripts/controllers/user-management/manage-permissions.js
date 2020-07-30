'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:UserManagementManagePermissionsCtrl
 * @description
 * # UserManagementManagePermissionsCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('UserManagementManagePermissionsCtrl', function ($scope, User, Role, $timeout, Settings) {

    // $scope.USERS_URL = Settings.API_ROOT + '/user/all/default';

    $scope.errors = [];

    $scope.userFilter = {name: ''};

    $scope.roleFilter = {name: ''};

    $scope.users = [];

    $scope.currentUserDn = '';

    $scope.userRoles = [];

    $scope.roles = [];

    $scope.getRoles = function(userDn) {
      $scope.errors = [];
      $scope.currentUserDn = userDn;
      User.query({dn: userDn, action: 'roles'}, function(userRoles) {
        $scope.userRoles = userRoles;
        for (var i = 0; i < $scope.roles.length; i++) {
          var idx = userRoles.map(function(x) { return x.dn; }).indexOf($scope.roles[i].dn);
          $scope.roles[i].selected = (idx != -1);
        }
      }, function(httpResponse) {
        $scope.errors.push(httpResponse.data);
      });
    };

    User.list(function(users) {
      $scope.users = users;
      var userDn = users[0].dn || $routeParams.dn;
      $scope.getRoles(userDn);
    }, function(httpResponse) {
      $scope.errors.push(httpResponse.data);
    });

    Role.query(function(roles) {
      $scope.roles = roles;
    }, function(httpResponse) {
      $scope.errors.push(httpResponse.data);
    });

    $scope.saveRoles = function() {
      $scope.success = '';
      $scope.errors = [];
      for (var i = 0; i < $scope.roles.length; i++) {
        var role = $scope.roles[i];
        var idx = $scope.userRoles.map(function(x) { return x.dn; }).indexOf(role.dn);
        if (role.selected && idx === -1) {
          Role.add({action: role.dn, udn: $scope.currentUserDn}, function(data){
            $scope.success = 'Role(s) successfully (un)assigned to user';
            //console.log(data);
          }, function(httpResponse){
            //console.log(httpResponse);
          });
        }

        if (!role.selected && idx != -1) {
          Role.remove({action: role.dn, udn: $scope.currentUserDn}, function(data){
            $scope.success = 'Role(s) successfully (un)assigned to user';
            //console.log(data);
          }, function(httpResponse){
            //console.log(httpResponse);
          });
        }
      }
    };

  });
