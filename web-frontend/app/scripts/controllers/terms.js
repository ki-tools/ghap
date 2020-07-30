'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:TermsCtrl
 * @description
 * # TermsCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('TermsCtrl', function ($scope, $cookies, $rootScope) {

    $scope.getKey = function(key) {
      var hash = window.location.hash.replace('?', '&');
      if (hash.indexOf('&' + key + '=') !== -1) {
        var pairs = hash.split('&');
        for (var i = 0; i < pairs.length; i++) {
          if (pairs[i].indexOf(key + '=') === 0) {
            var keyValue = pairs[i].split('=');
            return keyValue[1];
          }
        }
      }
      return null;
    };

    $scope.firstTime = $scope.getKey('firstTime');

    if($scope.firstTime){
      $rootScope.hideMenu = true;
    }

    $scope.resetPassword = function() {
      $rootScope.hideMenu = false;
      $cookies.put('ppolicyread', 'on');
      document.location.href = '/#/password-reset';
    };

  });
