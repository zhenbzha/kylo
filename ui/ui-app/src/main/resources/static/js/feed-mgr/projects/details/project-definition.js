define(['angular', 'feed-mgr/projects/module-name'], function (angular, moduleName) {
    /**
     * Manages the Project Definition section of the Project Details page.
     *
     * @constructor
     * @param $scope the application model
     * @param $mdDialog the Angular Material dialog service
     * @param $mdToast the Angular Material toast service
     * @param {AccessControlService} AccessControlService the access control service
     * @param ProjectsService the project service
     * @param StateService the URL service
     * @param FeedSecurityGroups the feed security groups service
     * @param FeedService the feed service
     */
    function ProjectDefinitionController($scope, $mdDialog, $mdToast, $q, $timeout, $window, AccessControlService, EntityAccessControlService, ProjectsService, StateService, FeedSecurityGroups, FeedService) {
        var self = this;

        /**
         * The Angular form for validation
         * @type {{}}
         */
        self.projectForm = {};

        /**
         * Prevent users from creating projects with these names
         * @type {string[]}
         */
        var reservedProjectNames = ['thinkbig']

        /**
         * Indicates if the project definition may be edited.
         * @type {boolean}
         */
        self.allowEdit = false;

        /**
         * Indicates the user has the permission to delete
         * @type {boolean}
         */
        self.allowDelete = false;

        /**
         * Project data used in "edit" mode.
         * @type {ProjectModel}
         */
        self.editModel = angular.copy(ProjectsService.model);

        /**
         * Indicates if the view is in "edit" mode.
         * @type {boolean} {@code true} if in "edit" mode or {@code false} if in "normal" mode
         */
        self.isEditable = !angular.isString(ProjectsService.model.id);

        /**
         * Project data used in "normal" mode.
         * @type {ProjectModel}
         */
        self.model = ProjectsService.model;

        this.projectSecurityGroups = FeedSecurityGroups;
        self.securityGroupChips = {};
        self.securityGroupChips.selectedItem = null;
        self.securityGroupChips.searchText = null;
        self.securityGroupsEnabled = false;
        self.systemNameEditable = false;

        FeedSecurityGroups.isEnabled().then(function (isValid) {
                self.securityGroupsEnabled = isValid;
            }
        );

        /**
         * System Name states:
         * !new 0, !editable 0, !feeds 0 - not auto generated, can change
         * !new 0, !editable 0,  feeds 1 - not auto generated, cannot change
         * !new 0,  editable 1, !feeds 0 - not auto generated, editable
         * !new 0,  editable 1,  feeds 1 - invalid state (cannot be editable with feeds)
         *  new 1, !editable 0, !feeds 0 - auto generated, can change
         *  new 1, !editable 0,  feeds 1 - invalid state (new cannot be with feeds)
         *  new 1,  editable 1, !feeds 0 - not auto generated, editable
         *  new 1,  editable 1,  feeds 1 - invalid state (cannot be editable with feeds)
         */
        self.getSystemNameDescription = function() {
            // console.log("self.isNewProject() = " + self.isNewProject());
            // console.log("self.isSystemNameEditable() = " + self.isSystemNameEditable());
            // console.log("self.hasFeeds() = " + self.hasFeeds());

            if (!self.isNewProject() && !self.isSystemNameEditable() && self.hasNoFeeds()) {
                return "Can be customised";
            }
            if (!self.isNewProject() && !self.isSystemNameEditable() && self.hasFeeds()) {
                return "Cannot be customised because Project has Feeds";
            }
            if (!self.isNewProject() && self.isSystemNameEditable() && self.hasNoFeeds()) {
                return "System name is now editable";
            }
            if (!self.isNewProject() && self.isSystemNameEditable() && self.hasFeeds()) {
                return ""; //invalid state, cannot be both editable and have feeds!
            }
            if (self.isNewProject() && !self.isSystemNameEditable() && self.hasNoFeeds()) {
                return "Auto generated from Project Name, can be customised";
            }
            if (self.isNewProject() && !self.isSystemNameEditable() && self.hasFeeds()) {
                return ""; //invalid state, cannot be new and already have feeds
            }
            if (self.isNewProject() && self.isSystemNameEditable() && self.hasNoFeeds()) {
                return "System name is now editable";
            }
            if (self.isNewProject() && self.isSystemNameEditable() && self.hasFeeds()) {
                return ""; //invalid state, cannot be new with feeds
            }
            return "";
        };

        self.isNewProject = function() {
            return self.editModel.id == undefined;
        };

        self.isSystemNameEditable = function() {
            return self.systemNameEditable;
        };

        // TODO: rip out this function
        self.hasFeeds = function() {
            return !self.hasNoFeeds();
        };

        // TODO: rip out this function
        self.hasNoFeeds = function() {
            return true;
            // return (!angular.isArray(self.model.relatedFeedSummaries) || self.model.relatedFeedSummaries.length === 0);
        };

        self.allowEditSystemName = function() {
            self.systemNameEditable = true;
            $timeout(function() {
                var systemNameInput = $window.document.getElementById("systemName");
                if(systemNameInput) {
                    systemNameInput.focus();
                }
            });
        };

        /**
         * Indicates if the project can be deleted.
         * @return {boolean} {@code true} if the project can be deleted, or {@code false} otherwise
         */
        self.canDelete = function () {
            return self.allowDelete && (angular.isString(self.model.id) && self.hasNoFeeds());
        };

        /**
         * Returns to the project list page if creating a new project.
         */
        self.onCancel = function () {
            self.systemNameEditable = false;
            if (!angular.isString(self.model.id)) {
                StateService.FeedManager().Project().navigateToProjects();
            }
        };

        /**
         * Deletes this project.
         */
        self.onDelete = function () {
            var name = self.editModel.projectName;
            ProjectsService.delete(self.editModel).then(function () {
                self.systemNameEditable = false;
                ProjectsService.reload();
                $mdToast.show(
                    $mdToast.simple()
                        .textContent('Successfully deleted the project ' + name)
                        .hideDelay(3000)
                );
                //redirect
                StateService.FeedManager().Project().navigateToProjects();
            }, function (err) {
                $mdDialog.show(
                    $mdDialog.alert()
                        .clickOutsideToClose(true)
                        .title('Unable to delete the project')
                        .textContent('Unable to delete the project ' + name + ". " + err.data.message)
                        .ariaLabel('Unable to delete the project')
                        .ok('Got it!')
                );
            });
        };

        /**
         * Switches to "edit" mode.
         */
        self.onEdit = function () {
            self.editModel = angular.copy(self.model);
        };

        /**
         * Check for duplicate display and system names.
         */
        self.validateDisplayAndSystemName = function() {
            var displayNameExists = false;
            var systemNameExists = false;
            var newDisplayName = self.editModel.projectName;

            FeedService.getSystemName(newDisplayName)
                .then(function (response) {
                    var systemName = response.data;
                    if (self.isNewProject() && !self.isSystemNameEditable()) {
                        self.editModel.systemName = systemName;
                    }

                    displayNameExists = _.some(ProjectsService.projects, function (project) {
                        return  (self.editModel.id == null || (self.editModel.id != null && project.id != self.editModel.id)) && project.projectName === newDisplayName;
                    });

                    systemNameExists = _.some(ProjectsService.projects, function (project) {
                        return  (self.editModel.id == null || (self.editModel.id != null && project.id != self.editModel.id)) && project.systemName === self.editModel.systemName;
                    });

                    var reservedProjectDisplayName = newDisplayName && _.indexOf(reservedProjectNames, newDisplayName.toLowerCase()) >= 0;
                    var reservedProjectSystemName = self.editModel.systemName && _.indexOf(reservedProjectNames, self.editModel.systemName.toLowerCase()) >= 0;

                    if (self.projectForm) {
                        if (self.projectForm['projectName']) {
                            self.projectForm['projectName'].$setValidity('duplicateDisplayName', !displayNameExists);
                            self.projectForm['projectName'].$setValidity('reservedProjectName', !reservedProjectDisplayName);
                        }
                        if (self.projectForm['systemName']) {
                            self.projectForm['systemName'].$setValidity('duplicateSystemName', !systemNameExists);
                            self.projectForm['systemName'].$setValidity('reservedProjectName', !reservedProjectSystemName);
                        }
                    }
                });
        };

        /**
         * Check for duplicate display and system names.
         */
        self.validateDisplayName = function() {
            var nameExists = false;
            var newName = self.editModel.projectName;

            FeedService.getSystemName(newName)
                .then(function (response) {
                    var systemName = response.data;
                    if (self.isNewProject() && !self.isSystemNameEditable()) {
                        self.editModel.systemName = systemName;
                    }

                    nameExists = _.some(ProjectsService.projects, function (project) {
                        return  (self.editModel.id == null || (self.editModel.id != null && project.id != self.editModel.id)) && project.projectName === newName;
                    });

                    var reservedProjectDisplayName = newName && _.indexOf(reservedProjectNames, newName.toLowerCase()) >= 0;

                    if (self.projectForm) {
                        if (self.projectForm['projectName']) {
                            self.projectForm['projectName'].$setValidity('duplicateDisplayName', !nameExists);
                            self.projectForm['projectName'].$setValidity('reservedProjectName', !reservedProjectDisplayName);
                        }
                    }
                });
        };

        /**
         * Check for duplicate display and system names.
         */
        self.validateSystemName = function() {
            var nameExists = false;
            var newName = self.editModel.systemName;

            FeedService.getSystemName(newName)
                .then(function (response) {
                    var systemName = response.data;

                    nameExists = _.some(ProjectsService.projects, function (project) {
                        return  (self.editModel.id == null || (self.editModel.id != null && project.id != self.editModel.id)) && project.systemName === systemName;
                    });

                    var reservedProjectSystemName = self.editModel.systemName && _.indexOf(reservedProjectNames, self.editModel.systemName.toLowerCase()) >= 0;
                    var invalidName = newName !== systemName;
                    if (self.projectForm) {
                        if (self.projectForm['systemName']) {
                            self.projectForm['systemName'].$setValidity('invalidName', !invalidName);
                            self.projectForm['systemName'].$setValidity('duplicateSystemName', !nameExists);
                            self.projectForm['systemName'].$setValidity('reservedProjectName', !reservedProjectSystemName);
                        }
                    }
                });
        };

        /**
         * Saves the project definition.
         */
        self.onSave = function () {
            var model = angular.copy(ProjectsService.model);
            model.projectName = self.editModel.projectName;
            model.systemName = self.editModel.systemName;
            model.description = self.editModel.description;
            model.icon = self.editModel.icon;
            model.iconColor = self.editModel.iconColor;
            model.notebookFolderEnabled = self.editModel.notebookFolderEnabled;

            var promise;
            if( self.editModel.hasOwnProperty('id') ) {
                promise = ProjectsService.doUpdate(model);
            } else {
                promise = ProjectsService.save(model);
            }

            promise.then(function (response) {
                self.systemNameEditable = false;
                ProjectsService.update(response.data);
                self.model = ProjectsService.model = response.data;
                $mdToast.show(
                    $mdToast.simple()
                        .textContent('Saved the Project')
                        .hideDelay(3000)
                );
                checkAccessPermissions();
            }, function (err) {
                $mdDialog.show(
                    $mdDialog.alert()
                        .clickOutsideToClose(true)
                        .title("Save Failed")
                        .textContent("The project '" + model.projectName + "' could not be saved. " + err.data.message)
                        .ariaLabel("Failed to save project")
                        .ok("Got it!")
                );
            });
        };

        /**
         * Shows the icon picker dialog.
         */
        self.showIconPicker = function () {
            var self = this;
            $mdDialog.show({
                controller: 'IconPickerDialog',
                templateUrl: 'js/common/icon-picker-dialog/icon-picker-dialog.html',
                parent: angular.element(document.body),
                clickOutsideToClose: false,
                fullscreen: true,
                locals: {
                    iconModel: self.editModel
                }
            })
                .then(function (msg) {
                    if (msg) {
                        self.editModel.icon = msg.icon;
                        self.editModel.iconColor = msg.color;
                    }
                });
        };

        self.getIconColorStyle = function(iconColor) {
            return  {'fill': iconColor};
        };

        function checkAccessPermissions() {
            //Apply the entity access permissions
            $q.when(AccessControlService.hasPermission(AccessControlService.PROJECTS_EDIT, self.model,
                AccessControlService.ENTITY_ACCESS.PROJECT.EDIT_PROJECT_DETAILS)).then(function (access) {
                self.allowEdit = access;
            });

            $q.when(AccessControlService.hasPermission(AccessControlService.PROJECTS_EDIT, self.model, AccessControlService.ENTITY_ACCESS.PROJECT.DELETE_PROJECT)).then(function (access) {
                self.allowDelete = access;
            });
        }

        checkAccessPermissions();

        // Fetch the existing projects
        ProjectsService.reload().then(function (response) {
            if (self.editModel) {
                self.validateDisplayName();
                self.validateSystemName();
            }
        });

        // Watch for changes to name
        $scope.$watch(
            function () {
                return self.editModel.projectName
            },
            self.validateDisplayName
        );
        // Watch for changes to system name
        $scope.$watch(
            function () {
                return self.editModel.systemName
            },
            self.validateSystemName
        );
    }

    /**
     * Creates a directive for the Project Definition section.
     *
     * @returns {Object} the directive
     */
    function thinkbigProjectDefinition() {
        return {
            controller: "ProjectDefinitionController",
            controllerAs: "vm",
            restrict: "E",
            scope: {},
            templateUrl: "js/feed-mgr/projects/details/project-definition.html"
        };
    }

    angular.module(moduleName).controller('ProjectDefinitionController',
        ["$scope", "$mdDialog", "$mdToast", "$q", "$timeout", "$window", "AccessControlService", "EntityAccessControlService", "ProjectsService", "StateService", "FeedSecurityGroups", "FeedService",
         ProjectDefinitionController]);
    angular.module(moduleName).directive('thinkbigProjectDefinition', thinkbigProjectDefinition);
});
