'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:RoleManagementAssignModelingActivitiesCtrl
 * @description
 * # RoleManagementAssignModelingActivitiesCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('RoleManagementAssignModelingActivitiesCtrl', function ($scope, Role, Activity, ActivityRoleAssociation, $timeout) {

    $scope.activityfilter = {activityName: ''};
    $scope.rolefilter = {name: ''};

    $scope.errors = [];
    $scope.roles = [];
    $scope.success = '&nbsp;';

    $scope.roleActivities = {};
    $scope.selectedRole = null;

    $scope.activities = [];

    Role.query(function(roles) {
      $scope.roles = roles;
    }, function(httpResponse) {
      $scope.errors.push(httpResponse.data);
    });

    Activity.query(function(activities) {
      $scope.activities = activities;
      $scope.roleActivities = {};

      angular.forEach(activities, function(a){
        $scope.roleActivities[a.id] = {activity: a, checked: null};
      });

    }, function(httpResponse) {
      $scope.errors.push(httpResponse.data);
    });

    $scope.getActivities = function(role) {
      $scope.errors = [];
      $scope.success = '&nbsp;';
      ActivityRoleAssociation.query({guid: role.guid}, function(roleActivities) {
        $scope.selectedRole = role;

        var map = {};
        angular.forEach(roleActivities, function(association){
          map[association.activityId] = association;
        });
        angular.forEach($scope.roleActivities, function(item, activityId){
          item.checked = (map[activityId] != null);
        });
      }, function(httpResponse) {
        $scope.errors.push(httpResponse.data);
      });
    };

    $scope.save = function save() {
      var role = $scope.selectedRole;
      var activities = [];
      angular.forEach($scope.roleActivities, function(item, activityId){
        if(item.checked)
          activities.push(item.activity);
      });
      // delete all associations for role
      ActivityRoleAssociation.delete({guid: role.guid}, function deleteAssociations(){
        ActivityRoleAssociation.save({guid: role.guid}, activities, function saveAccociations(){
          //console.log('ActivityRoleAssociation was saved')
          $scope.success = 'Activity-Role association was saved';
        });
      })

    };

  });
