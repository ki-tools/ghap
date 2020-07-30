'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:ForgotPasswordCtrl
 * @description
 * # ForgotPasswordCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('ForgotPasswordCtrl', function ($scope, User) {

  	$scope.userid = '';
  	$scope.requestSent = false;

    $scope.restore = function() {
      var idx = document.location.href.indexOf('#');
      var link = document.location.href.substring(0, idx);

      /*jshint unused:false*/
      User.save({action: 'password', dn: 'request', forgotPasswordEmail: $scope.userid}, link + '#/password-reset?token=$token$', function(user) {
      	 $scope.requestSent = true;
        },
        function error(httpResponse, status) {
          if(httpResponse.status === 404){
            //$scope.errors = [httpResponse.data];
            $scope.errors = ['Unknown Username'];
          }
          /*
          var errors = httpResponse.data.errors;
          for (var i = 0; i < errors.length; i++) {
            $scope.errors.push(errors[0].field + ': ' + errors[0].errors[0].message);
          }
          */
      });
    };

  });
