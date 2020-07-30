'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:RoleManagementCreateRolesCtrl
 * @description
 * # RoleManagementCreateRolesCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('RoleManagementCreateRolesCtrl', function ($scope, $routeParams, Role) {

    $scope.errors = [];

    $scope.roles = [];

    $scope.role = {};

    $scope.add = function() {
      $scope.success = '';
      $scope.errors = [];

      $scope.role.description = $scope.role.name;
      Role.save($scope.role, function(data) {
        $scope.success = 'Role ' + $scope.role.name + ' successfully added';
        $scope.reset();
        $scope.loadRoles();
      }, function(httpResponse) {
        var errors = httpResponse.data.errors;
        for (var i = 0; i < errors.length; i++) {
          $scope.errors.push(errors[0].field + ': ' + errors[0].errors[0].message);
        }
      });
    };

    $scope.reset = function() {
      $scope.role = {
        name:     ''
      };
    };

    $scope.loadRoles = function() {
      Role.query(function(roles) {
        $scope.roles = roles;
        var roleDn = $routeParams.dn || roles[0].dn;
        //Role.get({dn: roleDn});
      }, function(httpResponse) {
        //console.log(httpResponse.data);
      });
    };

    $scope.reset();
    $scope.loadRoles();

    // TODO: add check for $routeParams.dn and load user with this dn

  });
