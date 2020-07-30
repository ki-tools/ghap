'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:CreategroupCtrl
 * @description
 * # CreategroupCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('GroupManagementCreateGroupsCtrl', function ($scope, $routeParams, Group) {

    $scope.errors = [];

    $scope.groups = [];

    $scope.group = {};

    $scope.add = function() {
      $scope.success = '';
      $scope.errors = [];

      $scope.group.description = $scope.group.name;
      Group.save($scope.group, function(data) {
        $scope.success = 'Group ' + $scope.group.name + ' successfully added';
        $scope.reset();
        $scope.loadGroups();
      }, function(httpResponse) {
        var errors = httpResponse.data.errors;
        for (var i = 0; i < errors.length; i++) {
          $scope.errors.push(errors[0].field + ': ' + errors[0].errors[0].message);
        }
      });
    };

    $scope.reset = function() {
      $scope.group = {
        name:     '',
        dn: ''
      };
    };

    $scope.loadGroups = function() {
      Group.query(function(groups) {
        $scope.groups = groups;
        var groupDn = $routeParams.dn || (groups && groups.length > 0 ?groups[0].dn:null);
        if(groupDn){
          Group.get({dn: groupDn});
        }
      }, function(httpResponse) {
        //console.log(httpResponse.data);
      });
    };

    $scope.reset();
    $scope.loadGroups();

    // TODO: add check for $routeParams.dn and load user with this dn

  });
