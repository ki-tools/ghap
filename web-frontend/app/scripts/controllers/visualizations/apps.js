'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:VisualizationsCtrl
 * @description
 * # VisualizationsCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('VisualizationsAppsCtrl', function ($scope, Settings, $http) {

      $scope.app = {};
      $scope.query = null;
      $scope.search = search;
      $scope.setApps = setApps;

      var accessToken = localStorage.getItem('access_token');

      search();


      $scope.getAppUrl = function (app) {
        return Settings.SHINY_SERVER_URL + '/' + app.ApplicationRoot + '?token=' + accessToken;
      };

      $scope.getThumbnailUrl = function(applicationRoot, thumbnail) {
        return Settings.VISUALIZATION_URL + '/VisualizationPublisher/image?url=' + Settings.SHINY_SERVER_URL + '/' + thumbnail;
      };



      function search(){
        if($scope.query){
          $http
          .get(
            Settings.VISUALIZATION_URL + '/VisualizationPublisher/search',
            { params: {query: $scope.query} }
          )
          .success(setApps);
        }
        else {
          $http
          .get(
            Settings.VISUALIZATION_URL + '/VisualizationPublisher/registry',
            { params: {url: Settings.SHINY_SERVER_URL + '/registry.json?token=' + accessToken} }
          )
          .success(function(apps){
            apps = apps.map(function(app){
              return app.application;
            });
            setApps(apps);
          });
        }
      }

      function setApps(apps) {
        $scope.apps = apps;
      };

    });
