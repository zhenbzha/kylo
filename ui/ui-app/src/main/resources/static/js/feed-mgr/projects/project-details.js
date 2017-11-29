define(['angular', 'feed-mgr/projects/module-name'], function (angular, moduleName) {
    /**
     * Manages the Project Details page for creating and editing projects.
     *
     * @param $scope the application model
     * @param $transition$ the URL parameters
     * @param ProjectsService the project service
     * @constructor
     */
    function ProjectDetailsController($scope, $transition$, $q, ProjectsService, AccessControlService) {
        var self = this;

        /**
         * Indicates if the project is currently being loaded.
         * @type {boolean} {@code true} if the project is being loaded, or {@code false} if it has finished loading
         */
        self.loadingProject = true;

        self.showAccessControl = false;

        /**
         * Project data.
         * @type {ProjectModel}
         */
        self.model = {};
        $scope.$watch(
            function () {
                return ProjectsService.model
            },
            function (newModel, oldModel) {
                self.model = newModel;
                if (oldModel && oldModel.id == null && newModel.id != null) {
                    checkAccessControl();
                }
            },
            true
        );

        /**
         * Loads the project data once the list of projects has loaded.
         */
        self.onLoad = function () {
            if (angular.isString($transition$.params().projectId)) {
                self.model = ProjectsService.model = ProjectsService.findProject($transition$.params().projectId);
                /*if (angular.isDefined(ProjectsService.model)) {
                    ProjectsService.model.loadingRelatedFeeds = true;
                    ProjectsService.populateRelatedFeeds(ProjectsService.model).then(function (project) {
                        project.loadingRelatedFeeds = false;
                    });
                } */
                self.loadingProject = false;
            } else {
                self.loadingProject = false;
            }
        };

        self.getIconColorStyle = function (iconColor) {
            return {'fill': iconColor};
        };

        // Load the list of projects
        if (ProjectsService.projects.length === 0) {
            ProjectsService.reload().then(self.onLoad);
        } else {
            self.onLoad();
        }

        function checkAccessControl() {
            if (AccessControlService.isEntityAccessControlled()) {
                //Apply the entity access permissions... only showAccessControl if the user can change permissions
                $q.when(AccessControlService.hasPermission(AccessControlService.PROJECTS_ACCESS, self.model, AccessControlService.ENTITY_ACCESS.PROJECT.CHANGE_PROJECT_PERMISSIONS)).then(
                    function (access) {
                        self.showAccessControl = access;
                    });
            }
        }

        checkAccessControl();
    }

    angular.module(moduleName).controller('ProjectDetailsController', ["$scope", "$transition$", "$q", "ProjectsService", "AccessControlService", ProjectDetailsController]);
});
