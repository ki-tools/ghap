'use strict';

/**
 * @ngdoc directive
 * @name bmgfApp.directive:Workspace
 * @description
 * # Workspace
 */
angular.module('bmgfApp')
  .controller('WorkspaceCtrl', function($scope,
                                        Settings,
                                        User,
                                        UserData,
                                        Activity,
                                        Project,
                                        FileUploader,
                                        Grant,
                                        $timeout,
                                        $modal,
                                        $window) {

    $scope.workspaceError = '';
    $scope.programErrors = [];

    // UPLOAD TO USER WORKSPACE

    $scope.userData = [];
    $scope.uploader = null;
    $scope.path = '';
    $scope.someFilesChecked = false;
    $scope.isNewFolderDialogVisible = false;
    $scope.action = 'Actions';
    $scope.actions = ['Actions', 'Download', 'Delete'];
    $scope.uploading = false;

    $scope.$watch('userData', function(){
      $scope.someFilesChecked = false;
      for(var i in $scope.userData){
        if($scope.userData[i].checked){
          $scope.someFilesChecked = true;
          break;
        }
      }
    }, true);

    $scope.loadUserData = function loadUserData(){
      UserData.list($scope.user.guid, $scope.path, function queryUserDataCallback(userData){
        $scope.userData = userData;
        $scope.breadcrumb = breadcrumb();
      });
    };

    $scope.downloadPath = function(f){
      var accessToken = localStorage.getItem('access_token');
      if(!f.isDirectory){
        return Settings.USERDATA_ROOT+'/UserData/data/'+$scope.user.guid+'/'+f.path+(f.path ? '/':'') + f.name + '?token='+accessToken;
      }
    };

    $scope.downloadZipUrl = function downloadZipUrl(){
      return ($scope.userData && $scope.userData.length > 0 && $scope.someFilesChecked) ? UserData.downloadZipUrl($scope.user.guid, $scope.path, $scope.userData) : '';
    };

    $scope.createFolder = function createFolder(name){
      $scope.workspaceError = '';

      if (!name || name.trim().length === 0) {
        return;
      }

      //name = encodeURIComponent(name);
      var key;
      if (!$scope.path) {
        key = name;
      }
      else {
        //var path = $scope.path.split('/').map(function(str){return encodeURIComponent(str);}).join('/');
        key = $scope.path + '/' + name;
      }
      UserData.createFolder(
        $scope.user.guid,
        key,
        $scope.loadUserData,
        function(data, status){
          if (status === 409) {
            alertError('A folder named “' + key + '” already exists. <br>Please create a new folder with a different name.');
          } else if(status === 400){
            alertError('Folder name is invalid.  It should not contain the following characters <>:"/\\|?*. <br> Please enter a valid file name.');
          }
        }
      );
    };

    function alertError(str){
        $modal.open({
          templateUrl:       'views/directives/alert-error-modal.html',
          windowTemplateUrl: 'views/not-modal/window.html',
          controller:        'AlertErrorModalCtrl',
          backdrop:          true,
          scrollableBody:    true,
          resolve: {
            errorMessage: function(){return str;}
          }
        });
    }

    var breadcrumb = function breadcrumb(){
      var paths = [
        {
          name: '',
          path: '',
          isDirectory: true
        }
      ];
      var arr = $scope.path.split('/');
      var sub = '';
      for(var i in arr){
        var name = arr[i];
        if(name){
          paths.push({
            name: name,
            path: sub,
            isDirectory: true
          });

          sub = sub ? sub+'/'+name:name;
        }
      }
      return paths;
    };

    $scope.onUploadStart = function() {
      $scope.uploading = true;
    };

    $scope.onUploadComplete = function() {
      $scope.uploading = false;
      $scope.loadUserData();
    };

    $scope.initUploader = function initUploader(uuid){
      // create a uploader with options
      var accessToken = localStorage.getItem('access_token');
      $scope.uploader = new FileUploader({
          method:            'PUT',
          url:               Settings.USERDATA_ROOT + '/UserData/submit-location/' + uuid,
          autoUpload:        true,
          removeAfterUpload: true,
          headers:           {Authorization: 'Bearer ' + accessToken}
      });

      $scope.uploader.onAfterAddingAll = $scope.onUploadStart;
      $scope.uploader.onCompleteAll = $scope.onUploadComplete;

      $scope.uploader.onErrorItem = function(item, response, status, headers) {
        if (status === 409) {
          alertError('"' + item.file.name + '" file is already exists');
        } else {
          console.error('There was some error during file upload', item, response, status, headers);
          alertError('There were some errors during "' + item.file.name + '" file upload. See logs.');
        }
      };

      $scope.uploader.onBeforeUploadItem = function(item) {
          //item.formData.push({path: $scope.path});
          item.url = item.url + '?path=' + encodeURIComponent($scope.path);
      };

    };

    $scope.noSelection = function() {
      return $scope.userData.filter(function(f) { return f.checked === true; }).length === 0;
    };

    $scope.goTo = function(url) {
      //document.location.href = url;
      $window.location.assign(url);
    };

    $scope.downloadFiles = function downloadFiles() {
      var remove = function() {
        $(this).remove();
      };
      for(var i = 0; i < $scope.userData.length; i++){
        var f = $scope.userData[i];
        if(f.checked){
          var url = UserData.downloadZipUrl($scope.user.guid, $scope.path, f);
          $('<iframe>').attr('src', url).appendTo('body').load(remove);
          //$window.location.assign(url);
        }
      }
    };

    $scope.doFileAct = function() {
      switch($scope.action) {
        case 'Download':
          $scope.goTo($scope.downloadZipUrl());
          //$scope.downloadFiles();
          break;

        case 'Delete':
          var modalInstance = $modal.open({
            templateUrl:       'views/files-delete-confirm.html',
            windowTemplateUrl: 'views/not-modal/window.html',
            controller:        'FileDeleteConfirmModalCtrl',
            backdrop:          true,
            scrollableBody:    true,
          });

          modalInstance.result.then(function() {
            UserData.remove($scope.user.guid, $scope.path, $scope.userData, $scope.loadUserData);
          });
          break;
      }

      $scope.action = 'Actions';
    };

    $scope.toggleSelectAllFiles = function() {
      $scope.userData.forEach(function(f) { f.checked = $scope.selectAllFiles; });
    };

    $scope.allFilesSelected = function() {
      return $scope.userData.length > 0 &&
             $scope.userData.filter(function(f) { return f.checked === true; }).length === $scope.userData.length;
    };

    $scope.load = function load(f){
      if(f.isDirectory){
        $scope.path = f.path ? f.path+'/'+f.name:f.name;
        $scope.loadUserData();
      } else {
        //TODO download file
      }
    };




    // PROGRAM DATA ACCESS
    $scope.programs = [];
    $scope.userPrograms = [];
    $scope.userGrants = [];

    $scope.getGrants = function(program) {
      Project.getGrants({id: program.id}, function(grants) {
        program.grants = grants || [];
      });

      Project.getUserProgramGrants({id: program.id, userId: $scope.user.guid}, function(grants) {
        for (var i = 0; i < grants.length; i++) {
          $scope.userGrants[grants[i].id] = {
            readonly: (grants[i].permissions.length === 1 && grants[i].permissions[0] === 'READ')
          };
          if (!$scope.userPrograms[program.id]) {
            $scope.userPrograms[program.id] = {readonly: false};
          }
        }
      });
    };

    $scope.isUserProgram = function(program) {
      return $scope.userPrograms[program.id];
    };

    $scope.isReadonlyProgram = function(program) {
      return $scope.userPrograms[program.id] && $scope.userPrograms[program.id].readonly;
    };

    $scope.isReadonlyGrant = function(grant) {
      return $scope.userGrants[grant.id] && $scope.userGrants[grant.id].readonly;
    };

    $scope.loadUserProgramsAndGrants = function(user) {
      Project.query({}, function(programs) {
        $scope.programs = programs;
        for (var i = 0; i < programs.length; i++) {
          $scope.getGrants(programs[i]);
        }
      }, function(httpResponse) {
        $scope.programErrors.push(httpResponse.data);
      });

      Project.getUserPrograms({id: user.guid}, function(programs) {
        // $scope.userPrograms = programs.map(function(p){ return p.id; });
        for (var i = 0; i < programs.length; i++) {
          $scope.userPrograms[programs[i].id] = {
            readonly: (programs[i].permissions.length === 1 && programs[i].permissions[0] === 'READ')
          };
        }
      });
    };

    $scope.$watch('user', function(){
      if($scope.user !== null){
        $scope.initUploader($scope.user.guid);

        // load userdata
        $scope.loadUserData();

        $scope.loadUserProgramsAndGrants($scope.user);
      }
    });

    $scope.hoveredGrant = {};
    $scope.getChangeInfo = function(grant) {
      $scope.hoveredGrant = grant;
      if (grant.changes){
        return;
      }

      Grant.query({id: grant.id, action: 'history'}, function(changes) {
        grant.changes = changes || [];
      }, function(){
        grant.changes = [];
      });
    };

    $scope.onKeyPress = function($event) {
      if ($event.which === 13) {
        $scope.hideAndCreateFolder();
      }
    };

    $scope.hideAndCreateFolder = function() {
      $scope.createFolder($scope.newFolderName);
      $scope.isNewFolderDialogVisible = false;
    };

    $scope.toggleNewFolderDialog = function() {
      $scope.newFolderName = '';
      $scope.isNewFolderDialogVisible = !$scope.isNewFolderDialogVisible;
      if ($scope.isNewFolderDialogVisible) {
        $timeout(function() {
          document.getElementById('new-folder-input').focus();
        });
      }
    };

    $scope.toggleCopyUrlDialog = function(grant, $event) {
      grant.isCopyUrlDialogVisible = !grant.isCopyUrlDialogVisible;
      if (grant.isCopyUrlDialogVisible) {
        var el = $($event.target).parent().find('input');
        if(el && el.length) {
          el[0].setSelectionRange(0, el.val().length);
        }
      }
    };

    $scope.copyTextToClipboard = function copyTextToClipboard(grant) {
      grant.inCopyProcess = true;
      $timeout(function(){
        grant.inCopyProcess = false;
      },3000);
      var text = grant.cloneUrl;
      var textArea = document.createElement('textarea');
      //
      // *** This styling is an extra step which is likely not required. ***
      //
      // Why is it here? To ensure:
      // 1. the element is able to have focus and selection.
      // 2. if element was to flash render it has minimal visual impact.
      // 3. less flakyness with selection and copying which **might** occur if
      //    the textarea element is not visible.
      //
      // The likelihood is the element won't even render, not even a flash,
      // so some of these are just precautions. However in IE the element
      // is visible whilst the popup box asking the user for permission for
      // the web page to copy to the clipboard.
      //

      // Place in top-left corner of screen regardless of scroll position.
      textArea.style.position = 'fixed';
      textArea.style.top = 0;
      textArea.style.left = 0;

      // Ensure it has a small width and height. Setting to 1px / 1em
      // doesn't work as this gives a negative w/h on some browsers.
      textArea.style.width = '2em';
      textArea.style.height = '2em';

      // We don't need padding, reducing the size if it does flash render.
      textArea.style.padding = 0;

      // Clean up any borders.
      textArea.style.border = 'none';
      textArea.style.outline = 'none';
      textArea.style.boxShadow = 'none';

      // Avoid flash of white box if rendered for any reason.
      textArea.style.background = 'transparent';


      textArea.value = text;

      document.body.appendChild(textArea);

      textArea.select();


      var showUrl = function showUrl(text, url){
        $modal.open({
          templateUrl: 'views/directives/url-modal.html',
          controller: 'UrlModalCtrl',
          resolve: {
            url: function(){
              return url;
            },
            text: function(){
              return text;
            }
          }
        });
      };


      if (window.clipboardData) { // Internet Explorer
        window.clipboardData.setData('Text', text);
        //console.log('Text was copied via clipboardData');
      }
      else {
        if (window.netscape && window.netscape.security && window.netscape.security.PrivilegeManager) {
          window.netscape.security.PrivilegeManager.enablePrivilege ('UniversalXPConnect');
          //console.log('Permissions were defined for FF');
        }

        var successful = true;
        try {
          successful = document.execCommand('copy');
          // var msg = successful ? 'successful' : 'unsuccessful';
          //console.log('Copying text command was ' + msg);
        } catch (err) {
          successful = false;
          //console.log('Oops, unable to copy');
        }
        if(!successful){
          var result = new UAParser($window.navigator.userAgent).getResult();
          var cmd = (result.os && result.os.name === 'Mac OS') ? 'cmd-c':'ctrl-c';

          //console.log('Text to copy is "' + text + '"');
          showUrl('Press ' + cmd + ' to copy the text below', text);
        }
      }

      document.body.removeChild(textArea);
    };

  })
  .controller('UrlModalCtrl', function($scope, $modalInstance, $window, $timeout, text, url){
    $scope.url = url;
    $scope.text = text;

    $scope.cancel = function(){
      $modalInstance.dismiss('cancel');
    };
  })
  .controller('FileDeleteConfirmModalCtrl', function ($scope, $modalInstance) {
    $scope.del = function(){
      $modalInstance.close();
    };
    $scope.cancel = function(){
      $modalInstance.dismiss('cancel');
    };
  })

  .directive('workspace', function () {
    return {
      templateUrl: 'views/directives/workspace.html',
      restrict:    'E',
      controller:  'WorkspaceCtrl',
      scope:       { user: '=' }
    };
  })
  .controller('AlertErrorModalCtrl', function ($scope, $modalInstance, errorMessage, $sanitize, focus) {
    $scope.errorMessage = $sanitize(errorMessage);
    focus('openModal');
    $scope.cancel = function(){
      $modalInstance.dismiss('cancel');
    };
  })

;
