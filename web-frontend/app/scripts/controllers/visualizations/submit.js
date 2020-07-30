'use strict';

/**
 * @ngdoc function
 * @name bmgfApp.controller:VisualizationsSubmitCtrl
 * @description
 * # VisualizationsSubmitCtrl
 * Controller of the bmgfApp
 */
angular.module('bmgfApp')
  .controller('VisualizationsSubmitCtrl', function ($scope, Settings, FileUploader, User, Project) {

    $scope.user = null;
    $scope.appUploader = null;
    $scope.appUploaded = false;
    $scope.errors = [];
    $scope.success = [];

    $scope.resetMessages = resetMessages;
    $scope.setAppUploaded = setAppUploaded;
    $scope.setError = setError;
    $scope.initUserAndUpload = initUserAndUpload;

    $scope.initUploader = initUploader;

    $scope.projects = [];
    $scope.projectMap = {};
    $scope.grants = [];

    $scope.model = {
      SortPriority: 0,
      Updated: new Date()
    };

    $scope.$watch('model', function(newModel, oldModel){
      if( newModel.Project !== oldModel.Project ){

        var loadGrants = function loadActivities(project, callback){
          var id = $scope.projectMap[project];
          if(id){
            Project.getGrants({id: id}, function (grants) {
              if(grants){
                grants.forEach(function(grant, index, array) {
                  grant.project = project;
                });
              }
              callback(null,grants);
            }, function(){
              callback(null);
            });
          } else {
            callback(null);
          }
        };

        async.map(newModel.Project, loadGrants, function(e,r) {
          $scope.grants = (r === null || r.length === 0) ? [] : r.reduce(function (a, b) {
            return (a !== undefined) ? a.concat(b) : b;
          });
        });
      }
    }, true);

    $scope.$watch('grants', function(grants){
      var availableGrants = [];
      var ids = [];
      if(grants){
        availableGrants = grants.map(function(it){return {value: it.project + '/' + it.name, label: it.project + '/' + it.name} });
        ids = grants.map(function(it){return it.project + '/' + it.name});
      }
      $scope.schema.properties.Grant.items = availableGrants;

      if($scope.model.Grant){
        var grunts = $scope.model.Grant;
        for(var i = grunts - 1; i >= 0 ; i--){
          var grant = grunts[i];
          if(ids.indexOf(grant) < 0){
            grunts.splice(i, 1);
          }
        }
      }

      $scope.$broadcast('schemaFormRedraw');
    }, true);

    $scope.$watch('projects', function(projects){
      if(projects){
        $scope.schema.properties.Project.items = projects.map(function(it){return {value: it.key, label: it.name} });
        $scope.$broadcast('schemaFormRedraw');
      }
    });

    $scope.schema = {
      type: "object",
      properties: {
        ApplicationName: { type: "string", minLength: 3, title: "Application Name"},
        Description: { type: "string", minLength: 10},
        Author: {
          type: "string",
          "pattern": "^\\S+@\\S+$",
          "description": "Author Email should be provided"
        },
        Keyword: {
          "type": "string",
          title: "Keywords",
        },
        Project: {
          title: "Projects",
          type: 'array',
          format: 'uiselect',
          items: []
        },
        Grant: {
          title: "Grants",
          type: 'array',
          format: 'uiselect',
          items: []
        },
        Type: {
          type: "string",
          enum: ['Default','HTML']
        },
        Thumbnail: {
          type: "string"
        },
        ApplicationRoot: {
          type: "string",
          title: "Application Root"
        },
        SortPriority: {
          type: "integer",
          title: "Sort Priority"
        },
        Updated: {
          type: "string",
          "format": "date"
        }
      },
      "required": [
        "ApplicationName",
        "Description",
        "Author",
        "Thumbnail",
        "Keyword"
      ]
    };

    $scope.form = [
      "ApplicationName",
      "Description",
      "Author",
      {
        "key": "Keyword",
        notitle: true
      },
      "Project",
      "Grant",
      "Type",
      "Thumbnail",
      "SortPriority"
    ];


    $scope.onSubmit = function(form) {
      // First we broadcast an event so all fields validate themselves
      $scope.$broadcast('schemaFormValidate');

      // Then we check if the form is valid
      if (form.$valid) {
        $scope.appUploader.uploadAll()
      }
    }

    // activate controller(load user info)
    activate();


    function initUploader(uuid){
      var accessToken = localStorage.getItem('access_token');
      $scope.appUploader = new FileUploader({
          method:            'PUT',
          url:               Settings.VISUALIZATION_URL + '/VisualizationPublisher/publish',
          autoUpload:        false,
          removeAfterUpload: true,
          headers:           {Authorization: 'Bearer ' + accessToken}
      });

      $scope.appUploader.onAfterAddingAll = resetMessages;
      $scope.appUploader.onCompleteAll    = setAppUploaded;
      $scope.appUploader.onErrorItem      = setError;
      $scope.appUploader.onSuccessItem    = onSuccessItem;
      $scope.appUploader.onBeforeUploadItem = function onBeforeUploadItem(item) {
        item.formData.push({meta: JSON.stringify($scope.model)});
      };
    };


    function resetMessages() {
      $scope.appUploaded = false;
      $scope.errors = [];
      $scope.success = [];

      // allow to select only one file
      if ($scope.appUploader && $scope.appUploader.getNotUploadedItems().length > 1)
      {
        this.removeFromQueue(0);
      }
    };


    function setAppUploaded() {
      $scope.appUploaded = true;
    };


    function setError(item, response, status, headers) {
      //console.info('onErrorItem', item, response, status, headers);
      /*
      response = {
        "errors":[
          {"field":"Thumbnail","errors":[
            {
              "code":"wrong_ref",
              "message":"doesn't exist in the application archive"
            }
            ]
          }
        ]
      };
      */
      if(response.errors){
        var errors = response.errors;
        var fileErrors = {item: item, errors: []};
        for (var i = 0; i < errors.length; i++) {
          fileErrors.errors.push(errors[0].field + ' ' + errors[0].errors[0].message);
        }
        if(fileErrors.errors.length > 0){
          $scope.errors.push(fileErrors);
        }
      }
      else {
        $scope.errors.push({item: item, errors: ['There some issues during upload. See logs.']});
      }
    };

    function onSuccessItem(item, response, status, headers){
      $scope.success.push({item: item});
    }


    function initUserAndUpload(user){
      $scope.user = user;
      $scope.initUploader(user.guid);
      $scope.model.Author = user.email;

      Project.query({}, function(projects) {
        $scope.projects = projects;
        $scope.projectMap = projects.reduce(function(o, v, i) {
          o[v.key] = v.id;
          return o;
        }, {});
      });
    }

    function activate(){
      User.getCurrentUser($scope.initUserAndUpload);
    }

  });
