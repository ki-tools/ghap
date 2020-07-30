'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:AlertBannersCtrl
 * @description
 * # AlertBannersCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('AlertBannersCtrl', function ($scope,  $location, Banner, BodyShifter, $timeout, $rootScope) {

    var initialBanner = {startTime: '00:00', endTime: '00:00', color: '#9B242D'};

    $scope.banners = [];
    $scope.banner = angular.copy(initialBanner);
    $scope.banner.color = '#9B242D';
    $scope.errors = [];
    $scope.success = null;
    $scope.save = save; //update or create(if banner.id == null) banner
    $scope.select = select;
    $scope.reset = reset;
    $scope.remove = remove;
    $scope.actionType = 'Create';
    $scope.colorList = [{id: '#9B242D', name: '1-Red'},
                        { id: '#59452A', name: '2-Brown'},
                        { id: '#B6985E', name: '2-Tan'},
                        { id: '#CE6B29', name: '2-Orange'},
                        { id: '#977C00', name : '2-Gold'}];

    activate();

    function activate(){
      //load all banners
      loadAllBanners();
    }

    function loadAllBanners(){
      Banner.query({}, function success(banners) {
        $scope.banners = banners;
      }, function error(err) {
        // error handler
        console.error(err);
      });
    }

    function select(banner){
      $scope.banner = banner;
      $scope.actionType = 'Update';
      $scope.errors = [];
      $scope.success = null;
    };

    function reset(){
      $scope.success = '';
      $scope.banner = angular.copy(initialBanner);
      $scope.actionType = 'Create';
      focus('newBanner');
      $scope.form.$setPristine();
    };

    function remove(banner) {
      banner.$delete(function() {
        loadAllBanners();
        updateBannerDisplay();
      });
      reset();
    };

    /*
    function verify(form) {
      if(form.$invalid){
        return false;
      }

      $scope.errors = [];

      var startDate = Date.parse($scope.banner.startDate + "T" + $scope.banner.startTime + ":00.000Z");
      var endDate = Date.parse($scope.banner.endDate + "T" + $scope.banner.endTime + ":00.000Z");

      if (startDate > endDate) {
        $scope.errors.push("Start date is bigger than end date");
        return false;
      }

      return $scope.errors.length == 0;
    }
    */

    function updateBannerDisplay() {

      Banner.current(function(data){
        $rootScope.bannerMessages = data;
        $timeout(BodyShifter.shift, 0);
      }, function(error) {
        //jshint unused:false
      });
    }

    function showErrors(err){
      if (err.data != null && err.data.errors != null) {
        for (var ix in err.data.errors) {
          $scope.errors.push(err.data.errors[ix].msg);
        }
      }
    }

    function createBanner() {
      $scope.success = null;
      $scope.errors = [];
      if ($scope.form.$valid){
        var newBanner = new Banner($scope.banner);
        newBanner.$save(function(banner) {
          loadAllBanners();
          reset();
          $scope.success = 'Banner ' + banner.title + ' successfully created';
          updateBannerDisplay();
        }, showErrors);
      }
      else {
        $scope.errors.push("See validation errors above");
      }
    }

    function updateBanner() {
      $scope.success = null;
      $scope.errors = [];
      if ($scope.form.$valid){
        $scope.banner.$save(function success(banner, putResponseHeaders) {
          loadAllBanners();
          $scope.success = 'Banner ' + $scope.banner.title + ' successfully updated';
          updateBannerDisplay();
          //banner => saved banner object
          //putResponseHeaders => $http header getter
        }, showErrors);
      }
      else {
        $scope.errors.push("See validation errors above");
      }
    }


    function save(){
      switch($scope.actionType) {
        case 'Update':
            updateBanner();
          break;
        case 'Create':
            createBanner();
          break;
      }
    }

  });
