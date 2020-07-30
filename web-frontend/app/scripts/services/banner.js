'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.Banner
 * @description
 * # Banner
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
.factory('Banner', function ($resource, Settings) {
  var accessToken = localStorage.getItem('access_token');
  var res = $resource(Settings.BANNER_ROOT + '/:id?token=' + accessToken, {id : '@id'},
  	{
     	current: {method: 'GET', params: {id: 'current'}, cache: false, isArray: true}
  	}
  );

  return res;
});
