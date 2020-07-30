'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:NavCtrl
 * @description
 * # NavCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')

.controller('NavCtrl', function ($scope, $location, $rootScope, Nav, User, Settings, $http) {

  $scope.Nav = Nav;
  $scope.isInVpn = false;

  $http
  .get('/privateremoteaddr')
  .then(function(response) {
    //jshint unused:false

    //console.log(data);
    //console.log(data.indexOf('10.') === 0);
    //$scope.isInVpn = (response.data.indexOf('10.') === 0);
    $scope.isInVpn = true;// since OAuth proxy was implemented
  }, function(response){
    //jshint unused:false
    $scope.isInVpn = true;
  });

  $scope.isActive = function(route) {
    return ($location.path().indexOf(route) !== -1) ?  'current-menu-item' : '';
  };

  $scope.account = function() {
    $location.path('/my-account');
  };

  $scope.isUserLoggedIn = User.isUserLoggedIn;

  $scope.goDefault = function() {
    if (User.isUserLoggedIn()) {
      Nav.goDefaultUrl();
    }
  };

  $scope.isVisible = function(item) {
    return item.name !== 'Visualizations' || (item.name === 'Visualizations' && $scope.isInVpn);
  };

});
