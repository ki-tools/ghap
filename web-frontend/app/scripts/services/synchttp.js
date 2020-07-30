'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.syncHttp
 * @description
 * # syncHttp
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .provider('SyncHttp', function(){
    return {
      $get: function SyncHttp() {
        var syncGet = function get(url){
          var obj = null;
          var xhr = null;

          try {
            if (typeof XMLHttpRequest !== 'undefined') {
              xhr = new XMLHttpRequest();
            } else {
              xhr = new ActiveXObject('Msxml2.XMLHTTP');
            }
          } catch (e) {
            try {
              xhr = new ActiveXObject('Microsoft.XMLHTTP');
            } catch (E) {
              xhr = false;
            }
          }

          if (!xhr) {
            throw 'Can\'t init XMLHttpRequest';
          }

          //Bypassing the cache
          url = url + ((/\?/).test(url) ? '&' : '?') + (new Date()).getTime();

          xhr.open('GET', url, false);
          var accessToken = localStorage.getItem('access_token');
          if( accessToken ){
            xhr.setRequestHeader('Authorization', 'Bearer ' + accessToken);
          }


          try {
            xhr.send(null);

            if (xhr.status === 200) {
              obj = xhr.responseText;
            }
          } catch(err){
            //console.error('Error during GET "' + url + '". ' + err);
          }

          return obj;
        };

        return { get: syncGet };
      }
    };
  });
