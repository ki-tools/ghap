'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.VirtualResource
 * @description
 * # VirtualResource
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('VirtualResource', function ($resource, Settings) {
    var res =  $resource(Settings.PROVISIONING_ROOT + '/MultiVirtualPrivateGrid/get/compute/:id', {id:'@id'});
    return res;
  });