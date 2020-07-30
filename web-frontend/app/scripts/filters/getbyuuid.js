'use strict';

/**
 * @ngdoc filter
 * @name bmgfApp.filter:getByUuid
 * @function
 * @description
 * # getByUuid
 * Filter in the bmgfApp.
 */
angular.module('bmgfApp')
  .filter('getByUuid', function () {
	  return function(input, uuid) {
	    var i=0, len=input.length;
	    for (; i<len; i++) {
	      if (input[i].uuid === uuid) {
	        return input[i];
	      }
	    }
	    return null;
	  };
  });
