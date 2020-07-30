'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.DataSubmission
 * @description
 * # DataSubmission
 * Factory in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('DataSubmission', function ($resource, Settings) {
    var res = $resource(Settings.DATA_SUBMISSIONS_ROOT + '/DataSubmission/:action/:keyName',
    	{action: '@action', keyName: '@keyName'},
    	{}
    );

    var userData = [];

    res.getData = function getData(callback) {
      res.query({action: 'submissions'}, function(data){
        if( !angular.equals(userData, data) ){
          userData = data;
        }
        if(callback){
          callback(userData);
        }
      });
    };
    return res;
  });
