'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:newPassword
 * @description
 * # newPassword
 */
angular.module('bmgfApp')
  .controller('NewPasswordDirectiveCtrl', function ($scope) {
    $scope.hasUpperCase = false;
    $scope.hasLowCase = false;
    $scope.hasDigits = false;
    $scope.hasNonAlphas = false;
    $scope.moreThen7 = false;

    $scope.checkConds = function(){
      var val = $scope.password;
      $scope.hasUpperCase = val.match(/[A-Z]+/g) !== null;
      $scope.hasLowCase   = val.match(/[a-z]+/g) !== null;
      $scope.hasDigits    = val.match(/\d+/g) !== null;
      $scope.hasNonAlphas = val.match(/[!@#$%^&*(){}\[\]`~]/gi) !== null;
      $scope.moreThen7    = val.length > 7;
    };
  })
  .directive('newPassword', function () {
    return {
      templateUrl: 'views/directives/newpassword.html',
      restrict: 'E',
      scope: {
        isNew: '=',
        password: '=',
        passwordConfirmation: '='
      },
      controller: 'NewPasswordDirectiveCtrl'
    };
  });
