'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:SubmitdatasetctrlCtrl
 * @description
 * # SubmitdatasetctrlCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('SubmitDatasetCtrl', function ($scope, User, Role) {

    $scope.isDataContributor = false;

    $scope.isDataCurator = false;

    User.getCurrentUserRoles(function(roles){
      for (var i = 0; i < roles.length; i++) {
        if (roles[i].name === Role.buildInRoles.CURATOR) {
          $scope.isDataCurator = true;
        }

        if (roles[i].name === Role.buildInRoles.CONTRIBUTOR) {
          $scope.isDataContributor = true;
        }

        if (roles[i].name === Role.buildInRoles.ADMINISTRATOR) {
          $scope.isDataCurator = true;
          $scope.isDataContributor = true;
        }
      }
    });

  });
