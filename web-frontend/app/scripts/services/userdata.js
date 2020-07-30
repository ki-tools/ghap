'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.UserData
 * @description
 * # UserData
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('UserData', function ($resource, $http, Settings) {
    var res =  $resource(Settings.USERDATA_ROOT + '/UserData/:action/:guid/?path=:path',
      {guid:'@guid', action:'@action', path:'@path'},
      { 
        query:   {method: 'GET', params: {action: 'data-location'}, cache: false, isArray: true},
      }
    );

    res.createFolder = function (userGuid, name, callback, error) {
      $http.put(
        Settings.USERDATA_ROOT + '/UserData/folder/' + userGuid + '?path=' + encodeURIComponent(name),
        null,
        {
          cache: false,
          transformResponse: function(d) { return d; }
        }
      ).
      success(function(data) {
        callback(data);
      }).
      error(function(data, status) {
        if (error) {
          error(data, status);
        }
      });
    };

    res.list = function (userGuid, name, callback) {
      $http.get(Settings.USERDATA_ROOT + '/UserData/data-location/' + userGuid + '?path=' + encodeURIComponent(name), {cache: false}).
        success(function(data) {
          callback(data);
        })
        // .error(function(data, status) {
        //   console.error('Cannot create folder "' + name + '" for ' + userGuid + '. Response: ' + status);
        //   callback(null);
        // })
        ;
    };


    res.downloadZipUrl = function(uuid, path, files){
      if( !angular.isArray(files) ){
        files = [files];
      }
      var names = [];
      for(var i in files){
        var f = files[i];
        if(f.checked){
          names.push( 'file=' + encodeURIComponent(f.name) + (f.isDirectory ? '/':'') );
        }
      }

      var accessToken = localStorage.getItem('access_token');

      return Settings.USERDATA_ROOT+'/UserData/zip/'+uuid+'/'+'?token=' + accessToken + '&path=' + encodeURIComponent(path) + '&' + names.join('&');
    };

    res.remove = function(uuid, path, files, callback){
      if( files && files.length > 0 ){
        var names = [];
        for(var i in files){
          var f = files[i];
          if(f.checked){
            names.push(encodeURIComponent(f.name) + (f.isDirectory ? '/':''));
          }
        }

        if(names.length > 0){
          var url = Settings.USERDATA_ROOT+'/UserData/delete/'+ uuid + '?path=' + encodeURIComponent(path) + '&file=' + names.join('&file=');

          $http.delete(url, {cache: false}).
            success(function(data) {
              callback(data);
            })
            // .error(function(data, status) {
            //   console.error('Cannot delete files "' + names.join(', ') + '" for ' + uuid + '. Response: ' + status);
            //   callback(null);
            // })
            ;
        }
      }
    };

    return res;
  });
