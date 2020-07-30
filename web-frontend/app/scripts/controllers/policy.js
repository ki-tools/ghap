'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:PolicyCtrl
 * @description
 * # PolicyCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('PolicyCtrl', function ($location) {
  	$location.path('/terms');
  });
