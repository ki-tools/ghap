'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:ModelingActivitiesCtrl
 * @description
 * # ModelingActivitiesCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('ModelingActivitiesCtrl', function ($scope, User, Activity, $modal, $timeout, focus) {

    $scope.filter = {activityName: ''};

    $scope.activities = Activity.query();

    $scope.activity = {};

    $scope.select = function select(activity){
      $scope.activity = activity;
    };

    $scope.reset = function reset(){
      $scope.activity = {};
      focus('newActivity');
    };

    $scope.save = function save() {

      var newActivity    = $scope.activity.id ? $scope.activity : new Activity($scope.activity);
      var createOrUpdate = ($scope.activity.id ? newActivity.$save : newActivity.$create).bind(newActivity);

      createOrUpdate( function(){
        $scope.activity = {};
        $scope.activities = Activity.query();
      });

    };

    $scope.remove = function remove(activity) {

      var modalInstance = $modal.open({
        templateUrl:       'views/modeling-activities/delete-confirm.html',
        windowTemplateUrl: 'views/not-modal/window.html',
        controller:        'ActivityDeleteConfirmModalCtrl',
        backdrop:          true,
        scrollableBody:    true,
        resolve: {
          activity: function(){return activity;}
        }
      });

      modalInstance.result.then(function() {
        activity.$delete(function(){
          $scope.activities = Activity.query();
        });
      });

    };



  })
  .controller('ActivityDeleteConfirmModalCtrl', function ($scope, $modalInstance, activity) {
    $scope.activity = activity;
    $scope.del = function(){
      $modalInstance.close();
    };
    $scope.cancel = function(){
      $modalInstance.dismiss('cancel');
    };
  });
