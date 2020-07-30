'use strict';

/**
 * @ngdoc service
 * @name bmgfApp.Stack
 * @description
 * # Stack
 * Service in the bmgfApp.
 */
angular.module('bmgfApp')
  .factory('Report', function ($http, Settings) {

    var res = {};

    // /Reporting/getavailablereports  GET
    res.getAvailable = function getAvailable(data, success, error){
      $http
        .get(Settings.REPORT_API_URL + '/getavailablereports', data, {cache: false})
        .success(success || function(){})
        .error(error || function(){});
    };


    // /Reporting/create/{uuid}/{reportType} PUT
    res.create = function create(uuid, reportType, data, success, error){
      var json = {
        'clazz':      'io.ghap.reporting.data.DateRangeConstraint',
        'type':       'DATE_RANGE',
        'constraint': data
      };
      $http
        .put(
            Settings.REPORT_API_URL + '/create/' + uuid + '/' + reportType,
            json,
            {
                cache: false,
                transformResponse: function(d) { return d; }
            }
        )
        .success(success || function(){})
        .error(error || function(){});
    };

    // /Reporting/constrainedcreate/{uuid}/{reportType} PUT
    res.constrainedCreate  = function create(uuid, reportType, data, success, error){
      var json = [{
        'type':       'DATE_RANGE',
        'constraint': data
      }];
      $http
        .put(
            Settings.REPORT_API_URL + '/constrainedcreate/' + uuid + '/' + reportType,
            json,
            {
                cache: false,
                transformResponse: function(d) { return d; }
            }
        )
        .success(success || function(){})
        .error(error || function(){});
    };

    // /Reporting/removereport/{token} DELETE
    res.remove = function remove(token, success, error){
      $http
        .delete(Settings.REPORT_API_URL + '/removereport/' + token, {data: null, cache: false})
        .success(success || function(){})
        .error(error || function(){});
    };

    // /Reporting/getreports GET
    res.get = function get(data, success, error){
      $http
        .get(Settings.REPORT_API_URL + '/getreports', data, {cache: false})
        .success(success || function(){})
        .error(error || function(){});
    };

    // /Reporting/getuserreports/{uuid} GET
    res.getUserReports = function getUserReports(uuid, data, success, error){
      $http
        .get(Settings.REPORT_API_URL + '/getuserreports/' + uuid, data, {cache: false})
        .success(success || function(){})
        .error(error || function(){});
    };

    // /Reporting/getreport/{token} GET
    res.getByToken = function getReport(token, data, success, error){
      $http
        .get(Settings.REPORT_API_URL + '/getreport/' + token, data, {cache: false})
        .success(success || function(){})
        .error(error || function(){});
    };

    // /Reporting/getstatus/{token} GET
    res.getStatus = function getStatus(token, data, success, error){
      $http
        .get(Settings.REPORT_API_URL + '/getstatus/' + token, data, {cache: false})
        .success(success || function(){})
        .error(error || function(){});
    };

    // /Reporting/getreportcontent GET
    res.getContent = function getContent(token, data, success, error){
      $http
        .get(Settings.REPORT_API_URL + '/getreportcontent/' + token, data, { cache: false })
        .success(success || function(){})
        .error(error || function(){});
    };

    res.getContentUrl = function getContent(token){
      var accessToken = localStorage.getItem('access_token');
      return Settings.REPORT_API_URL + '/getreportcontent/' + token + '?token=' + accessToken;
    };

    // /Reporting/getstatus GET
    res.getStatus = function getStatus(token, data, success, error){
      $http
        .get(Settings.REPORT_API_URL + '/getstatus/' + token, data, { cache: false })
        .success(success || function(){})
        .error(error || function(){});
    };

    // /Reporting/getstatuses 
    res.getStatuses = function getStatuses(data, success, error){

        $http
            .post(Settings.REPORT_API_URL + '/getstatuses', data, { cache: false })
            .success(success || function(){})
            .error(error || function(){});


        /*
        var query = '?tokens=' + data.join("&tokens=")

        $http
            .get(Settings.REPORT_API_URL + '/getstatuses' + query, { cache: false })
            .success(success || function(){})
            .error(error || function(){});
            */
    };

    return res;
  });
