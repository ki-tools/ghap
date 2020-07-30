'use strict';

// early access to access_token
(function(){
  var hash = window.location.hash;
  if (hash.indexOf('access_token') !== -1) {
    var accessToken = '';
    var pairs = hash.replace('?', '&').split('&');
    for (var i = 0; i < pairs.length; i++) {
      if (pairs[i].indexOf('access_token') !== -1) {
        var keyValue = pairs[i].split('=');
        accessToken = keyValue[1];
        break;
      }
    }

    localStorage.setItem('access_token', accessToken);
  }
})();

angular.module('pascalprecht.translate').factory('$translateStaticFilesLoader',['$q','$http',function(a,b){
  return function(c){
    if(!c||!angular.isString(c.prefix)||!angular.isString(c.suffix)){
      throw new Error('Couldn\'t load static files, no prefix or suffix specified!');
    }
    var d=a.defer();
    return b({
      url:[c.prefix,c.key,c.suffix].join(''),
      method:'GET',
      params:''
    }).success(function(a){
      d.resolve(a);
    }).error(function(){
      d.reject(c.key);
    }),d.promise;};
  }]
);

/**
 * @ngdoc overview
 * @name bmgfApp
 * @description
 * # bmgfApp
 *
 * Main module of the application.
 */
angular
  .module('bmgfApp', [
    'ui.bootstrap',
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch',
    'ui.uploader',
    'angularFileUpload',
    'bgf.paginateAnything',
    'ui.bootstrap.position',
    'ui.bootstrap.bindHtml',
    'ui.bootstrap.tooltip',
    'ui.bootstrap.popover',
    'angular.filter',
    'ui.select',
    'schemaForm',
    'pascalprecht.translate'
  ])
  .run(function($rootScope, Auth, $location, Nav, Settings, User, Banner, $interval, BodyShifter, $timeout, $window) {
    if (
      window.location.hash.indexOf('access_token') !== -1 &&
      window.location.hash.indexOf('password-reset') === -1 &&
      window.location.hash.indexOf('terms') === -1
    ) {
      //window.location.search = ''; // do not uncomment it!!!! Ask Andrew instead
      Nav.goDefaultUrl();
    }

    $window.onresize = BodyShifter.shift;

    $rootScope.$watch(
      function() { return $location.path(); },
      Nav.getSubmenus
    );

    $rootScope.inactiveMinutes = 0;
    $rootScope.activateSessionTimeoutCheck = true;

    Banner.current(function(data){
        $rootScope.bannerMessages = data;
        $timeout(BodyShifter.shift, 0);
    }, function(error) {
        //jshint unused:false
    });

    $interval(function(){
      $rootScope.inactiveMinutes++;
      if ($rootScope.activateSessionTimeoutCheck && $rootScope.inactiveMinutes === 30) {
        if(window.alert){
          window.alert('Session is expired. Please relogin.');
        }
        Nav.logout();
      }
    }, 60 * 1000);

    $rootScope.$on('$routeChangeStart', function (event, next) {
      $rootScope.inactiveMinutes = 0;

      if (next.access !== undefined) {
        Auth.authorize(
          next.access.requiresLogin,
          next.access.requiredRoles,
          next.access.roleCheckType,
          function(result) {
            if (result === 'loginRequired') {
              User.dropCurrentUser();
              document.location.href = Settings.OAUTH_URL + '/oauth/authorize?client_id=projectservice&response_type=token&redirect_uri=' + document.location.href;
            } else if (result === 'notAuthorised') {
              User.accessDeniedRedirect();
            }
          }
        );
      }
    });
  })

  .config(function ($routeProvider, $httpProvider, $compileProvider, $translateProvider, pathsProvider) {

    $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|ftp|mailto|data):/);
    $httpProvider.defaults.withCredentials = true;

    /*jshint unused:false*/
    $httpProvider.interceptors.push(function($q, Settings, $rootScope) {
      return {
        'request' : function(config) {
          var accessToken = localStorage.getItem('access_token');

          if (accessToken) {
            if ((typeof config.withCredentials  === 'undefined') || config.withCredentials) {
              config.headers.Authorization = 'Bearer ' + accessToken;
            }

          }

          if (!accessToken) {
            localStorage.removeItem('current_user');
            localStorage.removeItem('current_user_roles');
          }
          return config || $q.when(config);
        },
        'responseError' : function(response) {
          // see: http://stackoverflow.com/questions/16081267/xmlhttprequest-status-0-instead-of-401-in-ie-10
          if (response.status === 401) {
            // console.log('got error with token ' + localStorage.getItem('access_token'));

            localStorage.removeItem('current_user');
            localStorage.removeItem('current_user_roles');
            document.location.href = Settings.OAUTH_URL + '/oauth/authorize?client_id=projectservice&response_type=token&redirect_uri=' + document.location.href;
          }
          return $q.reject(response);
        }
      };
    });

    $translateProvider.useStaticFilesLoader({
      prefix: '/locales/locale-',
      suffix: '.json'
    });
    $translateProvider.preferredLanguage('en');

    angular.forEach(pathsProvider.paths, function(value) {
      $routeProvider
      .when(value.path, {
        templateUrl: value.templateUrl,
        controller:  value.controller,
        access:      value.access
      });
    });
    $routeProvider.otherwise({
      redirectTo: '/my-account',
      access: { requiresLogin: true }
    });
  })
;
